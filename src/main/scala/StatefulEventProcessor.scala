import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.{
  GroupState,
  GroupStateTimeout,
  OutputMode
}
import org.apache.spark.sql.types._

case class Event(
    name: String,
    id: Option[Long] = None,
    processing_time: Option[Long] = None,
    source_time: Option[Long] = None
)
case class OutputEvent(name: String, id: Long, freshnessTime: Long)
case class EventState(
    name: String,
    processingTime: Option[Long] = None,
    sourceTime: Option[Long] = None,
    freshnessTime: Option[Long] = None
)

object StatefulEventProcessor {

  private def computeMax(
      leftOpt: Option[Long],
      rightOpt: Option[Long]
  ): Option[Long] = {
    (leftOpt, rightOpt) match {
      case (Some(left), Some(right)) => Some(math.max(left, right))
      case (Some(left), None)        => Some(left)
      case (None, Some(right))       => Some(right)
      case (None, None)              => None
    }
  }

  private def updateEventState(
      key: String,
      values: Iterator[Event],
      state: GroupState[EventState]
  ): Iterator[OutputEvent] = {
    val currentState =
      if (state.exists) state.get else EventState(key, None)

    values.flatMap(value => {
      val latestProcessingTime =
        computeMax(value.processing_time, currentState.processingTime)
      val latestSourceTime =
        computeMax(value.source_time, currentState.sourceTime)
      val times = Seq(
        latestProcessingTime,
        latestSourceTime
      ).flatten
      val freshnessTime = Some(if (times.nonEmpty) times.min else 0L)
      val latestFreshnessTime =
        computeMax(freshnessTime, currentState.freshnessTime)
      val newState = EventState(
        value.name,
        latestProcessingTime,
        latestSourceTime,
        latestFreshnessTime
      )
      state.update(newState)
      value.id.map(v =>
        OutputEvent(
          value.name,
          v,
          latestFreshnessTime.get
        )
      )
    })
  }

  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("StatefulEventProcessor")
      .master("local[2]")
      .config("spark.driver.bindAddress", "127.0.0.1")
      .getOrCreate()

    import spark.implicits._

    val processingSchema = StructType(
      Array(
        StructField("name", StringType, false),
        StructField("id", LongType, false),
        StructField("processing_time", LongType, false)
      )
    )

    val sourceSchema = StructType(
      Array(
        StructField("name", StringType, false),
        StructField("id", LongType, false),
        StructField("source_time", LongType, false)
      )
    )

    val socketDF = spark.readStream
      .format("socket")
      .option("host", "127.0.0.1")
      .option("port", 9999)
      .load()

    val sourceDF = spark.readStream
      .format("socket")
      .option("host", "127.0.0.1")
      .option("port", 9998)
      .load()

    val eventDS = socketDF
      .select(from_json(col("value"), processingSchema).as("event"))
      .select("event.*")
      .select(
        col("name"),
        col("id").cast(LongType),
        col("processing_time").cast(LongType),
        lit(null).cast(LongType).as("source_time")
      )
      .as[Event]

    val sourceDS = sourceDF
      .select(from_json(col("value"), sourceSchema).as("event"))
      .select("event.*")
      .select(
        col("name"),
        lit(null).cast(LongType).as("id"),
        lit(null).cast(LongType).as("processing_time"),
        col("source_time").cast(LongType)
      )
      .as[Event]

    val unionDS = eventDS.union(sourceDS)

    val statefulDS = unionDS
      .groupByKey(_.name)
      .flatMapGroupsWithState(
        OutputMode.Update(),
        GroupStateTimeout.NoTimeout()
      )(updateEventState)

    val query = statefulDS.writeStream
      .outputMode(OutputMode.Update())
      .format("console")
      .option("truncate", false)
      .trigger(
        org.apache.spark.sql.streaming.Trigger.ProcessingTime("15 seconds")
      )
      .start()

    query.awaitTermination()
  }
}
