# DiningPhilosphers
University lab work | Variants on the 'Dining Philosophers Problem' in concurrent programming which demonstrates deadlock

Variant 1: a right-handed philosopher
In the standard version of the dining philosophers, all the philosophers are left-handed: they pick up their left fork first. Implement a variant where one of the philosophers is right-handed, i.e. she picks up her right fork first.
Variant 2: using a butler
Now consider a variant using an extra process, which represents a butler. The butler makes sure that no more than four philosophers are ever simul- taneously seated.
Variant 3: using timeouts
Now consider a variant where, if a philosopher is unable to obtain her second fork, she puts down her first fork, and retries later.
In order to implement this idea, you will need to use a DeadlineManyOne channel. The command
c = new channel.DeadlineManyOne[T]
creates such a channel. It is like a ManyOne[T] channel, except that it has an
additional command
c.writeBefore(nsWait)(value)
which tries to send value for up to nsWait nanoseconds, but if the message isn’t received within that time, then it times out and returns. The command returns a boolean, indicating whether value was successfully received.
A DeadlineManyOne channel may not be used in an alt. To avoid using an alt, you can provide each fork with a single channel, from which it can receive a message from either of its philosophers (hence the channel has to
1
be many-one). Also, a DeadlineManyOne[T] channel does not extend the ?[T] and ![T] traits (because those traits represent ports that can be used in an alt). This means that you can’t parameterise processes using those traits.
If we reach a state where all philosophers are blocked, it’s possible that they all put down their forks at the same time, and then all retry at the same time, leading to them being blocked again. How can we avoid this (with high probability)?
