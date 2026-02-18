java FuelStationSimulation [A] [B] [C] [D] [E] [F]

[A] Trips: How many rounds each vehicle makes before retiring.
[B] Docks: How many parking spots the station has (The "Checkouts").
[C] Max Nitrogen: Size of the station's N2 tank.
[D] Max Quantum: Size of the station's QF tank.
[E] Regular Vehicles: Number of customers (Consumers).
[F] Supply Vehicles: Number of tanker trucks (Producers).

### Core Efficiencies
1. Atomic Deposit+Refuel (Dock Retention)
   - Why: Prevents "race conditions" where another vehicle steals the dock between the supply truck depositing fuel and requesting return fuel.
   - How: The supply vehicle never releases the dock handle. It deposits, takes its cut, and *then* leaves.

2. Scannable FIFO Queue
   - Why: Prevents "Head-of-Line Blocking". If the first 10 cars can't be served (empty tank), the station skips them to find the supply truck (which *can* be served).
   - How: The `isFirstSatisfiable` method iterates the queue to find the first "doable" job.

3. Optimized Logging
   - Why: Prevents "Lock Starvation". Regular cars were spamming the console inside the lock, preventing the supply truck from ever entering the station.
   - How: Moved heavy printing outside the critical `wait()` loops.