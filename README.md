## Seriously Safe, Efficient and Fast Order Matching System

This project is aimed to provide a safe, efficient and fast order matching system. By safe, I mean the project must
persist every trade crash-safely (not just provide an in-memory matching engine). By efficient, I mean the project
should use resources efficiently, and reach reasonable benchmark numbers in peak performance. In Java, using resources
efficiently requires some custom implementations, like low level memory access and managing memory manually, thereby in
implementation I tried to use these kinds of techniques where possible. By fast, I mean the processing of incoming
orders at network level, matching orders and persistence should all be fast and no bottleneck is acceptable.

#### Network

Pure java is used to provide a high performance asynchronous and non-blocking TCP level implementation for network
stack. Also, a custom binary protocol is designed for messaging, and the network implementation directly uses that
protocol to parse them.

#### Persistence

A custom storage implementation is used in both asynchronous and synchronous manners to provide a solid context for
working with files and file storage, including an atomic WAL implementation.

#### Performance

The project is able to match 10000 crash-safe-persisted orders per symbol (ex. BTC/USDT) per second. For example if you
have two active symbols, on each symbol you easily can reach 10K matches, meaning 20K total. One core per symbol or one
core per multiple symbols may affect these numbers. And also, the GC pressure is tried to be minimal, for example
off-heap memory with manual control over allocation and de-allocation is nearly used all over the packages to gain
better performance numbers.

---

## Current Features

- Price/Time algorithm
- Market & Limit orders
- Cancel order
- Fetch order book with optional depth parameter
- Remaining at both order and trade level recording
- Replay from day 0
