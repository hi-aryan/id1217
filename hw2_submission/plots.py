import matplotlib.pyplot as plt

# Hardcoded data based on your logs
runs = [
    {
        "filename": "benchmark_run_1.png",
        "title": "N-Queens Benchmark (Max 1 Thread)",
        "threads": [1],
        "times":   [0.040886],
        "speedup": [1.00]
    },
    {
        "filename": "benchmark_run_2.png",
        "title": "N-Queens Benchmark (Max 2 Threads)",
        "threads": [1, 2],
        "times":   [0.040770, 0.020879],
        "speedup": [1.00, 1.95]
    },
    {
        "filename": "benchmark_run_4.png",
        "title": "N-Queens Benchmark (Max 4 Threads)",
        "threads": [1, 2, 3, 4],
        "times":   [0.040924, 0.020855, 0.015139, 0.011165],
        "speedup": [1.00, 1.96, 2.70, 3.67]
    },
    {
        "filename": "benchmark_run_8.png",
        "title": "N-Queens Benchmark (Max 8 Threads)",
        "threads": [1, 2, 3, 4, 5, 6, 7, 8],
        "times":   [0.041448, 0.020807, 0.015066, 0.011186, 0.008982, 0.007582, 0.006497, 0.005802],
        "speedup": [1.00, 1.99, 2.75, 3.71, 4.61, 5.47, 6.38, 7.14]
    },
    {
        "filename": "benchmark_run_10.png",
        "title": "N-Queens Benchmark (Max 10 Threads)",
        "threads": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
        "times":   [0.040918, 0.020870, 0.015140, 0.011292, 0.009122, 0.007480, 0.006533, 0.005790, 0.005167, 0.004675],
        "speedup": [1.00, 1.96, 2.70, 3.62, 4.49, 5.47, 6.26, 7.07, 7.92, 8.75]
    },
    {
        "filename": "benchmark_run_12.png",
        "title": "N-Queens Benchmark (Max 12 Threads)",
        "threads": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],
        "times":   [0.040995, 0.020830, 0.015064, 0.011306, 0.008911, 0.007497, 0.006454, 0.005781, 0.005191, 0.004700, 0.004448, 0.004266],
        "speedup": [1.00, 1.97, 2.72, 3.63, 4.60, 5.47, 6.35, 7.09, 7.90, 8.72, 9.22, 9.61]
    },
    {
        "filename": "benchmark_run_16.png",
        "title": "N-Queens Benchmark (Max 16 Threads)",
        "threads": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16],
        "times":   [0.040940, 0.020823, 0.015097, 0.011133, 0.008905, 0.007577, 0.006514, 0.005837, 
                    0.005182, 0.004699, 0.004573, 0.004349, 0.004109, 0.004044, 0.010567, 0.011024],
        "speedup": [1.00, 1.97, 2.71, 3.68, 4.60, 5.40, 6.29, 7.01, 
                    7.90, 8.71, 8.95, 9.41, 9.96, 10.12, 3.87, 3.71]
    }
]

def create_plot(run_data):
    # Create a figure with 2 side-by-side subplots
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(12, 5))
    
    # Plot 1: Execution Time
    ax1.plot(run_data["threads"], run_data["times"], marker='o', color='tab:blue')
    ax1.set_title("Execution Time vs Threads")
    ax1.set_xlabel("Number of Threads")
    ax1.set_ylabel("Time (seconds)")
    ax1.grid(True)
    
    # Plot 2: Speedup
    ax2.plot(run_data["threads"], run_data["speedup"], marker='o', color='tab:green', label='Actual Speedup')
    
    # Add an "Ideal Linear Speedup" line for comparison
    ax2.plot(run_data["threads"], run_data["threads"], linestyle='--', color='gray', alpha=0.5, label='Ideal Linear')
    
    ax2.set_title("Speedup vs Threads")
    ax2.set_xlabel("Number of Threads")
    ax2.set_ylabel("Speedup Factor")
    ax2.legend()
    ax2.grid(True)

    # Main Title
    fig.suptitle(f"{run_data['title']} (N=12)", fontsize=14)
    
    # Save file
    plt.tight_layout()
    plt.savefig(run_data["filename"])
    print(f"Generated: {run_data['filename']}")
    plt.close()

# Loop through each run and create the image
for run in runs:
    create_plot(run)