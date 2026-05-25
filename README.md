Choreographed Saga Order & Inventory Transaction Processing System
This repository implements a highly scalable, decoupled, and reliable distributed transaction processing system using Java 17, Spring Boot, and Apache Kafka. 
The system utilizes an asynchronous, event-driven architecture based on the Saga Pattern (Choreography) to manage order placement and real-time inventory allocation.

🏛️ System Architecture
The solution is divided into two decoupled microservices that communicate entirely through event streams via Apache Kafka:

Order Service: Exposes a REST API to accept transactions, persists pending orders to a relational database, and acts as an event producer.

Inventory Service: Acts as an event-driven processor that listens to order events, evaluates and updates stock levels safely, and publishes validation feedback back into the ecosystem.

Plaintext
               +-----------------------------------+
               |        HTTP REST Client           |
               +-----------------+-----------------+
                                 |
                        (POST /GET Requests)
                                 v
               +-----------------------------------+
               |           Order Service           |
               |  (Resilience4j & Relational DB)   |
               +-----------------+-----------------+
                                 |
                     (Publishes OrderPlacedEvent)
                                 v
               +-----------------------------------+
               |   Kafka: order-placed-topic       |
               +-----------------+-----------------+
                                 |
                     (Consumes OrderPlacedEvent)
                                 v
               +-----------------------------------+
               |         Inventory Service         |
               |  (Processes & Deducts Stock DB)   |
               +-----------------+-----------------+
                                 |
                   (Publishes InventoryCheckedEvent)
                                 v
               +-----------------------------------+
               |  Kafka: inventory-checked-topic   |
               +-----------------+-----------------+
                                 |
                  (Consumes Event to Update Order)
                                 v
                       [ Order Service State ]
🛠️ Key Design Decisions
1. Asynchronous Choreographed Saga over Orchestration
Instead of keeping a REST connection open while checking inventory (which degrades availability and limits throughput), we used an event-driven Choreographed Saga Pattern:

Why: Removing synchronous HTTP communication between services eliminates cascading failures. If the Inventory Service experiences transient downtime, the Order Service can still accept incoming customer requests unimpeded.

2. Microservice Partitioning via Kafka Keys
When the Order Service publishes an OrderPlacedEvent, the orderId is explicitly passed as the Kafka Message Key.

Why: Passing the identifier as the routing key ensures that all transactional steps associated with a single order are consistently hashed to the exact same Kafka Topic Partition. This guarantees strict message sequencing, eliminating race conditions when scaling out consumer instances (groupId = "inventory-processor-group").

3. Resilience4j Fault Tolerance Boundaries
The primary order creation API endpoint is heavily guarded using Resilience4j wrappers:

@CircuitBreaker: Prevents cascading network saturation by failing fast if internal components or Kafka cluster linkages become degrade or unreachable.

@Retry & Safe Fallbacks: Provides automatic retry mechanisms for temporary infrastructure drops. If failures persist, the transaction steps down cleanly into predictable fallback loops (createOrderFallback), yielding graceful service degradation.

4. Manual Offset Acknowledgments (AckMode.MANUAL)
Why: Relying on default automatic offsets risks declaring data as "processed" even if a service crashes mid-transaction. By switching to manual offsets (ack.acknowledge()), the system guarantees At-Least-Once delivery. Offset pointers only advance after local stock deductions commit and feedback packets dispatch successfully.

📌 Architectural Assumptions
Stock Initialization: It is assumed that products have baseline quantities pre-populated in the Inventory system before orders are placed.

Event-Driven Consistency: The architecture accepts Eventual Consistency. Orders temporarily stay in a PENDING_INVENTORY_CHECK state and update asymptotically once the reactive consumer reports loop validation back to the service layer.

Idempotency: The downstream database applies strict constraints on entity keys ensuring that duplicate events pushed by Kafka network retries do not lead to double stock-deductions.

🚀 Build, Run, and Setup Instructions
Prerequisites
Java 17 installed (JDK 17)

Maven 3.x or Gradle

A running Apache Kafka instance (Local or via Docker)

Step 1: Spin Up Infrastructure (Kafka Broker)
	Step 1: Prerequisites & Download
	Verify Java: Ensure Java 17+ is installed. Open Command Prompt and type java -version to confirm.

	Download Kafka: Visit the official Apache Kafka Downloads page. Under Binary downloads, download the latest Scala package (e.g., kafka_2.13-3.x.x.tgz).

	Extract Files: Extract the .tgz contents to a clean, short path to avoid Windows file path limits (e.g., extract directly to C:\kafka).

	Step 2: Review Single Broker Configuration
	By default, Apache Kafka comes pre-configured to run a single broker right out of the box.

	You can review or adjust the settings by opening C:\kafka\config\server.properties. The default parameters are perfectly optimized for a single-node setup:

		Properties
		broker.id=0
		listeners=PLAINTEXT://localhost:9092
		log.dirs=/tmp/kafka-logs
		zookeeper.connect=localhost:2181
		(Optional: If you want to keep data inside your installation folder instead of the Windows temp directory, change log.dirs=/tmp/kafka-logs to log.dirs=C:/kafka/kafka-logs).

	Step 3: Start the Kafka Environment
	You need to open 2 separate Command Prompt windows to run the single broker infrastructure. Keep both windows open while testing.

		Window 1: Start ZooKeeper (The Coordinator)
		Navigate to your Kafka root folder and execute the ZooKeeper server script:
		DOS
		cd C:\kafka
		.\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties
		
		Window 2: Start the Kafka Broker
		Open a new Command Prompt window, navigate to your root folder, and start the single broker:
		DOS
		cd C:\kafka
		.\bin\windows\kafka-server-start.bat .\config\server.properties
		
	Step 4: Create and Verify Your Topic
	Now that your single broker is running on port 9092, create a topic tailored for a standalone instance. Since you only have 1 broker, your replication factor must be set to 1.

	Open a 3rd Command Prompt window to run these validation utilities:

	1. Create the Topic
		Create a topic for your application events with 3 partitions for processing parallelism:
		DOS
		.\bin\windows\kafka-topics.bat --create --topic order-placed-topic --bootstrap-server localhost:9092 --replication-factor 1 --partitions 3
		
	2. Verify Topic Status
	Verify that the single broker has successfully registered and hosted the partitions:
		DOS
		.\bin\windows\kafka-topics.bat --describe --topic order-placed-topic --bootstrap-server localhost:9092
	In the output, you will see that all partitions are assigned to Leader: 0 (your single broker).

Step 2: Build and Run Inventory Service
Navigate to the inventory project folder:

Bash
cd inventory
mvn clean install
mvn spring-boot:run

Step 3: Build and Run Order Service
Navigate to the order project folder:

Bash
cd order
mvn clean install
mvn spring-boot:run

📊 Sample API Usage
1. Submit a Transaction (Create Order)
Endpoint: POST /v1/orders

Headers: Content-Type: application/json

Sample Request Body:

JSON
{
  "customerId": 7842,
  "productId": 202,
  "quantity": 3
}
Sample Response Body (202 Accepted / 201 Created):

JSON
{
  "orderId": 10523,
  "customerId": 7842,
  "productId": 202,
  "quantity": 3,
  "orderStatus": "PENDING_INVENTORY_CHECK",
  "createdAt": "2026-05-25T11:30:00"
}
2. Retrieve Transaction Status
Endpoint: GET /v1/orders/10523

Sample Response Body:

JSON
{
  "orderId": 10523,
  "customerId": 7842,
  "productId": 202,
  "quantity": 3,
  "orderStatus": "SUCCESSFUL_ALLOCATION",
  "createdAt": "2026-05-25T11:30:00"
}
🧪 Testing Strategies
The solution is supported by an automated test suite emphasizing decoupled component validations:

Unit Testing (JUnit 5 + Mockito): Core validation handlers (e.g., OrderServiceTest and OrderProcessServiceTest) are fully isolated. Real connections to Kafka and Databases are substituted with mocks to guarantee fast, deterministic behavior.

Asynchronous Future Stubbing: Tests natively simulate internal thread waits (kafkaTemplate.send().get()) using CompletableFuture.completedFuture() wrappers to assert payload data processing properties under low-latency simulations.

To Execution Coverage Suite: Run the complete test deck using Maven:

Bash
mvn test
