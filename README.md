### Seriously Safe, Efficient and Fast Order Matching System

This project is aimed to provide a safe, efficient and fast order matching system without any compromise in fairness
which is very important in OMS software implementations.

#### Safe

By being safe, I mean the project must persist every trade crash-safely (not just provide an in-memory matching engine).
Simultaneously it must maintain the fairness of transactions and the transparency of transaction execution. As a
customer, Did you ever doubt about fairness of your buy/sell orders in a crypto exchange platform you use?!

#### Efficient

By being efficient, I mean the project should use resources efficiently and reach reasonable benchmark numbers in peak
loads. Using resources efficiently requires some custom implementations, like low level memory access and managing
memory manually, thereby in implementation, I tried to use these kinds of techniques where ever possible. As an exchange
platform founder, Did you ever been sensitized about how much efficient your main software components work?! Does your
platform works well under heavy loads?! With how much cost and resource usage?

#### Fast

By being fast, I mean the processing of incoming orders, matching orders, persistence and fetching order books should
all be fast and no bottleneck is acceptable. Why? Because you should compete with other platforms out there. Are your
customers happy with you?! Or they complain about your platform's being slow sometimes?!

---

### How achieved these goals?

#### Network & Protocol Level

Pure java is used to provide a high performance asynchronous and non-blocking TCP level implementation for network
stack. Also, a custom binary protocol is designed for messaging, and the network implementation directly uses that
protocol to parse them.

#### Storage & Persistence Level

A custom storage implementation is used in both asynchronous and synchronous manners to provide a solid context for
working with files and file storage, including an atomic WAL implementation.

#### CPU & Memory Level

The project supports both one-symbol per core or multiple-symbols per core architecture. The GC pressure is tried to be
minimal, for example off-heap memory with manual control over allocation and de-allocation is nearly used all over the
packages to gain better performance numbers.

#### What about performance numbers?

The project is able to match 10000 crash-safe persisted orders per symbol (ex. BTC/USDT) per second. For example if you
have two active symbols, on each symbol you easily can reach 10K matches, meaning 20K total. One-symbol per core or
multiple-symbols per core may affect these numbers.

---

### Current Features

- Price/Time algorithm
- Limit order (GTC, IOC & FOK)
- Market order (IOC & FOK)
- Order canceling
- Partially order cancelling
- Order book fetching with optional depth parameter
- Remaining recording, at both order and trade models
- JFR enabled to be able to visualize detailed metrics about project's behavior at runtime

### Under Development Features

- Stop & StopLimit orders

---

### Development Setup (macOS)

- JDK 25
    - ```curl -s "https://get.sdkman.io" | bash```
    - ```sdk install java 25-graalce```
- LZ4 Compression Library
    - ```/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"```
    - ```brew install lz4```
- Docker Engine (Docker Desktop)
    - See https://www.docker.com to follow the installation instructions.

First of all you need to start the docker engine (docker desktop). Then after, set the following environment variable:

```bash
export LIBRARIES_NATIVE_LZ4_PATH="/the/path/to/liblz4.dylib"
```

For example:

```bash
export LIBRARIES_NATIVE_LZ4_PATH="/opt/homebrew/Cellar/lz4/1.9.4/lib/liblz4.dylib"
```

At final step run:

```bash
./mvnw clean package
```

You are now ready to import the project into your IDE to develop/test further.

---

### Restful API server

See [Gate](https://github.com/lirezap/Gate) for more information about restful API server and its usage.
