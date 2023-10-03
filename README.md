
# Distributed Dining Philosophers Problem
This repository contains an implementation for solving the classic Dining Philosophers problem in a peer-to-peer (P2P) environment, using the Ricart-Agrawala Algorithm for mutual exclusion with the Roucairol-Carvalho optimization along with Lamport's Logical Clocks.

## Overview
<img src="https://raw.githubusercontent.com/itakurah/Distributed-Dining-Philosophers-Problem/main/images/table.jpg" width=50% height=50%>

Sources:
- *https://www.flaticon.com/free-icon/philosophy_2178189*
- *https://www.freepik.com/icon/fork-diagonal_45433*


## [Ricart-Agrawala Algorithm](https://en.wikipedia.org/wiki/Ricart%E2%80%93Agrawala_algorithm "Ricart-Agrawala Algorithm")
**Requesting Site**

- Sends a message to all philosophers. This message includes the philosopher's id, and the current timestamp of the system according to its [Lamport clock](https://en.wikipedia.org/wiki/Lamport_timestamp "Lamport clock") (_which is assumed to be synchronized with the other sites_)

**Receiving Philosopher**
- Upon reception of a request message, immediately sending a timestamped _reply_ message if and only if:
    - the receiving philosopher is not currently interested in the critical section OR
    - the receiving philosopher has a lower timestamp
- Otherwise, the receiving philosopher will defer the reply message. This means that a reply will be sent only after the receiving philosopher has finished using the critical section itself.

**Critical Section:**
- Requesting philosopher enters its critical section only after receiving all reply messages.
- Upon exiting the critical section, the philosopher sends all deferred reply messages.

### Roucairol-Carvalho optimization
Once site $P_i$ has received a *reply* message from site $P_j$, site $P_i$ may enter the critical section multiple times without receiving permission from $P_j$ on subsequent attempts up to the moment when $P_i$ has sent a *reply* message to $P_j$.

## [Lamport Clock](https://en.wikipedia.org/wiki/Lamport_timestamp "Lamport Clock")

**Algorithm:**

-   **Happened before relation(->):** $a \rightarrow b$, means 'a' happened before 'b'.
-   **Logical Clock:** The criteria for the logical clocks are:
    -   [C1]: $C_i(a) < C_i(b)$, [ $C_i$ -> Logical Clock, If 'a' happened before 'b', then time of 'a' will be less than 'b' in a particular process. ]
-   [C2]: $C_i(a) < C_j(b)$, [ Clock value of $C_i(a)$ is less than $C_j(b)$ ]

**Reference:**

-   **Philosopher:** $P_i$
-   **Event:** **$E_{ij}$**, where $i$ is the process in number and $j$: **$j^{th}$** event in the **$i^{th}$** process.
-   **$tm$:** vector time span for message $m$.
-   **$C_i$** vector clock associated with process **$P_i$**, the **$j^{th}$** element is **$C_i[j]$** and contains **$P_i$'s** latest value for the current time in process **$P_j$**.
-   **$d$:** drift time = 1.

**Implementation Rules[IR]:**

-   **[IR1]:** If $a \rightarrow b$ [‘a’ happened before ‘b’ within the same process] then, **$C_i(b) = C_i(a) + d$**
-   **[IR2]:** $C_j = \max(C_j, tm + 1)$ [If there's more number of processes, then $tm = \text{value of } C_i(a)$, $C_j = \max \text{ value between } C_j \text{ and } tm + d$]
## [G-Counter (Grow-only Counter)](https://en.wikipedia.org/wiki/Conflict-free_replicated_data_type "G-Counter")
The G-Counter is designed to maintain a counter for a cluster comprising 'n' nodes. Within this cluster, each node is allocated a unique ID ranging from 0 to n - 1. Consequently, every node is granted its own designated slot in the 'P' array, which it independently increments. Updates are continuously disseminated in the background and subsequently merged by determining the maximum value from all elements within the 'P' array. The 'merge' function is characterized by being commutative, associative, and idempotent. The 'query' function returns the total sum of the counter.

# Sources
- https://en.wikipedia.org/wiki/Conflict-free_replicated_data_type
- https://en.wikipedia.org/wiki/Ricart%E2%80%93Agrawala_algorithm
- https://en.wikipedia.org/wiki/Lamport_timestamp
