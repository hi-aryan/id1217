CXX = g++
CXXFLAGS = -Wall -pthread -std=c++11
TARGET = fuel_station

OBJS = main.o FuelStation.o

all: $(TARGET)

$(TARGET): $(OBJS)
	$(CXX) $(CXXFLAGS) -o $(TARGET) $(OBJS)

main.o: main.cpp FuelStation.h
	$(CXX) $(CXXFLAGS) -c main.cpp

FuelStation.o: FuelStation.cpp FuelStation.h
	$(CXX) $(CXXFLAGS) -c FuelStation.cpp

clean:
	rm -f $(TARGET) $(OBJS)

run: $(TARGET)
	./$(TARGET) 100 100 3 5 2 3
