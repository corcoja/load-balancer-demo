# Load Balancer Simulator <!-- omit in toc -->

- [Description](#description)
  - [Types of load balancers](#types-of-load-balancers)
- [Dependencies](#dependencies)
  - [Compile Maven Dependencies](#compile-maven-dependencies)
  - [Test Maven Dependencies](#test-maven-dependencies)
- [Usage](#usage)
  - [Build with Maven](#build-with-maven)
  - [Test with Maven](#test-with-maven)
  - [Generate Javadoc with Maven](#generate-javadoc-with-maven)
- [License](#license)

## Description

A load balancer is a component that, once invoked, distributes incoming requests to a list of registered providers and returns the value obtained from one of the registered providers to the original caller. This project intents to simulate the behavior of two types of load balancers with providers that create artificial load when invoked.

### Types of load balancers

- [Randomized Load Balancer][1]
- [Round Robin Load Balancer][2]

## Dependencies

### Compile Maven Dependencies

1. Apache Log4j API `2.17.2` ([`log4j-api`][3])
2. Apache Log4j Core `2.17.2` ([`log4j-core`][4])

### Test Maven Dependencies

1. JUnit Jupiter API `5.8.2` ([`junit-jupiter-api`][5])
2. JUnit Jupiter Engine `5.8.2` ([`junit-jupiter-engine`][6])
3. JUnit Jupiter Params `5.8.2` ([`junit-jupiter-params`][7])
4. Awaitility `4.2.0` ([`awaitility`][8])

## Usage

### Build with Maven

```bash
# Install
mvn install

# ...or compile
mvn compile

# ...or package
mvn package
```

### Test with Maven

Unit tests cover all the scenarios for request load balancing for both types of load balancers implemented in the project.

```bash
# Test
mvn test
```

> **NOTE:** Since the unit tests simulate various scenarios for Load Balancing, running all the tests might take a long time (approximately 1 minute on an Apple M1 powered computer with 32 GB of RAM).

### Generate Javadoc with Maven

In the base directory of the project, run the command below to generate the project's Javadoc to a directory in `target/site`.

```bash
# Generate Javadoc
mvn javadoc:javadoc
```

## License

All of the work is licensed under the [MIT License][9], unless specified otherwise due to constraints by dependencies.

[1]: https://en.wikipedia.org/wiki/Load_balancing_(computing)#Randomized_static "Randomized static load balancing"
[2]: https://en.wikipedia.org/wiki/Round-robin_scheduling "Round-robin scheduling"
[3]: https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-api "log4j-api"
[4]: https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core "Apache Log4j Core"
[5]: https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api "JUnit Jupiter API"
[6]: https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-engine "JUnit Jupiter Engine"
[7]: https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-params "JUnit Jupiter Params"
[8]: https://mvnrepository.com/artifact/org.awaitility/awaitility "Awaitility"
[9]: https://opensource.org/licenses/MIT "The MIT License"
