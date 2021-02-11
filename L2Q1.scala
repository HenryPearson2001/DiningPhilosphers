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
  val Pick = true; val Drop = false

  val log = new io.threadcso.debug.Log[String](N)

  /** A single  left handed philosopher. */
  def leftHandedPhil(me: Int, left: ![Command], right: ![Command]) = proc("Phil"+me){
    repeat{
      Think
      log.add(me, me+" sits"); Pause
      left!Pick; log.add(me, me+" picks up left fork"); Pause
      right!Pick; log.add(me, me+" picks up right fork"); Pause
      log.add(me, me+" eats"); Eat
      left!Drop; log.add(me, me+" drops left fork"); Pause; right!Drop; log.add(me, me+" drops right fork"); Pause
      log.add(me, me+" leaves")
      if(me == 0) print(".")
    }
  }

  /** A single philosopher. */
  def rightHandedPhil(me: Int, left: ![Command], right: ![Command]) = proc("Phil"+me){
    repeat{
      Think
      log.add(me, me+" sits"); Pause
      right!Pick; log.add(me, me+" picks up right fork"); Pause
      left!Pick; log.add(me, me+" picks up left fork"); Pause
      log.add(me, me+" eats"); Eat
      right!Drop; log.add(me, me+" drops right fork"); Pause; left!Drop; log.add(me, me+" drops left fork"); Pause;
      log.add(me, me+" leaves")
      if(me == 0) print(".")
    }
  }

  /** A single fork. */
  def fork(me: Int, left: ?[Command], right: ?[Command]) = proc("Fork"+me){
    serve(
      left =?=> {
        x => assert(x == Pick); val y = left?; assert(y == Drop)
      }
      |
      right =?=> {
        x => assert(x == Pick); val y = right?; assert(y == Drop)
      }
    )
  }

  /** The complete system. */
  val system = {
    // Channels to pick up and drop the forks:
    val philToLeftFork, philToRightFork = Array.fill(N)(OneOne[Command])
    // philToLeftFork(i) is from Phil(i) to Fork(i);
    // philToRightFork(i) is from Phil(i) to Fork((i-1)%N)
    val allPhils = || (
      for (i <- 1 until N)
      yield leftHandedPhil(i, philToLeftFork(i), philToRightFork(i))
    ) || rightHandedPhil(0, philToLeftFork(0), philToRightFork(0))
    val allForks = || (
      for (i <- 0 until N) yield
        fork(i, philToRightFork((i+1)%N), philToLeftFork(i))
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

