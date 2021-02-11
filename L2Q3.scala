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
  type Command = Boolean
  type Command2 = (Side, Boolean)
  type Side = String
  val Pick = true; val Drop = false

  val log = new io.threadcso.debug.Log[String](N)

  /** A single philosopher. */
  def phil(me: Int, left: ![Command], right: ![Command], forkChanL: channel.DeadlineManyOne[Command2], forkChanR: channel.DeadlineManyOne[Command2]) = proc("Phil"+me){
    repeat{
      Think
      log.add(me, me+" sits"); Pause
      var success1 = false
      while (!success1) {
          // pick up left fork - keep retrying until successful
          var success = forkChanL.writeBefore(Random.nextInt(300))(("left", Pick))
          while (!success) success = forkChanL.writeBefore(Random.nextInt(300))(("left", Pick))
          log.add(me, me+" picks up left fork"); Pause
          // pick up right fork
          success = forkChanR.writeBefore(Random.nextInt(300))(("right", Pick))
          // if success, eat. otherwise drop left fork and retry
          if (success) {
            success1 = true
            log.add(me, me+" picks up right fork"); Pause
          }
          else {
            left!Drop; log.add(me, me+" drops left fork");
            // wait before retrying
            Pause
          }
      }
      log.add(me, me+" eats"); Eat
      left!Drop; log.add(me, me+" drops left fork"); Pause; right!Drop; log.add(me, me+" drops right fork"); Pause
      log.add(me, me+" leaves")
      if(me == 0) print(".")
    }
  }

  /** A single fork - receives pick messages via philsChan and drop messages via left and right */
  def fork(me: Int, left: ?[Command], right: ?[Command], philsChan: channel.DeadlineManyOne[Command2]) = proc("Fork"+me){
    repeat {
      val (s, c) = philsChan?()
      assert(c == Pick)
      if (s == "right") { val l = left?(); assert(l == Drop) }
      else { val r = right?(); assert(r == Drop) }
    }
  }

  /** The complete system. */
  val system = {
    // Channels to pick up and drop the forks:
    val philDropLeftFork, philDropRightFork = Array.fill(N)(OneOne[Command])
    val philPickFork = Array.fill(N)(new channel.DeadlineManyOne[Command2])
    // philToLeftFork(i) is from Phil(i) to Fork(i);
    // philToRightFork(i) is from Phil(i) to Fork((i-1)%N)
    val allPhils = || (
      for (i <- 0 until N)
      yield phil(i, philDropLeftFork(i), philDropRightFork(i), philPickFork((i+1)%N), philPickFork(i))
    )
    val allForks = || (
      for (i <- 0 until N) yield
        fork(i, philDropRightFork((i+1)%N), philDropLeftFork(i), philPickFork((i+1)%N))
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

