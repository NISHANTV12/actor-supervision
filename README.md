Actor Supervision
=========================

A minimal example demonstrating actor supervision in typed and untyped actors

Parent needs to watch a child explicitly. Creating an actor does not entitle automatic watch.

Terminated message can't be forwarded to another actor, since that actor might not be watching the subject. 
Instead, if you need to forward Terminated to another actor you should send the information in your own message. 
