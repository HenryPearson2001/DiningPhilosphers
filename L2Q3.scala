import io.threadcso._
import scala.language.postfixOps
import scala.util.Random

/** Simulation of the Dining Philosophers example. */
object PhilsLog{
  val N = 5 // Number of philosophers

  // Simulate basic actions
  def Eat = Thread.sleep(500)
  def Think = Thread.sleep(Random.nextInt(900))
  def Pause = Thread.sleep(500)

  // Each philosopher will send "pick" and "drop" commands to her forks, which
  // we simulate using the following values.
  type Command = (Side, Boolean)
  type Command2 = (Side, Boolean)
  type Side = String
  val Pick = true; val Drop = false

  val log = new io.threadcso.debug.Log[String](N)

  /** A single philosopher. */
  def phil(me: Int, left: ![Command], right: ![Command], forkChanL: DeadlineManyOne[Command2], forkChanR: DeadlineManyOne[Command2]) = proc("Phil"+me){
    repeat{
      Think
      log.add(me, me+" sits"); Pause
      // wait random time to pick up left fork
      forkChanL.writeBefore(Random.nextInt(900))(("left", Pick))
      log.add(me, me+" picks up left fork"); Pause
      // wait random time to pick up right fork
      forkChanR.writeBefore(Random.nextInt(900))(("right", Pick))
      log.add(me, me+" picks up right fork"); Pause
      log.add(me, me+" eats"); Eat
      left!Drop; Pause; right!Drop; Pause
      log.add(me, me+" leaves")
      if(me == 0) print(".")
    }
  }

  /** A single fork - receives pick messages via philsChan and drop messages via left and right */
  def fork(me: Int, left: ?[Command], right: ?[Command], philsChan: DeadlineManyOne[Command2]) = proc("Fork"+me){
    repeat {
      val (s, c) = philsChan?()
      assert(c = pick)
      if s == "right" { val r = right?(); assert(r == Drop) }
      else { val l = left?(); assert(l == Drop) }
    }
  }

  /** The complete system. */
  val system = {
    // Channels to pick up and drop the forks:
    val philDropLeftFork, philDropRightFork = Array.fill(N)(OneOne[Command])
    val philPickLeftFork, philPickRightFork = Array.fill(N)(DeadlineManyOne[Command2])
    // philToLeftFork(i) is from Phil(i) to Fork(i);
    // philToRightFork(i) is from Phil(i) to Fork((i-1)%N)
    val allPhils = || (
      for (i <- 0 until N)
      yield phil(i, philDropLeftFork(i), philDropRightFork(i), philPickLeftFork(i),  )
    )
    val allForks = || (
      for (i <- 0 until N) yield
        fork(i, philDropLeftFork((i+1)%N), philDropRightFork(i), philPickLeftFork((i+1)%N), philPickRightFork(i))
    )
    allPhils || allForks
  }

  /** Run the system. */
  def main(args : Array[String]) = {
    log.writeToFileOnShutdown("philsLog.txt")
    run(system)
    exit
  }
}

