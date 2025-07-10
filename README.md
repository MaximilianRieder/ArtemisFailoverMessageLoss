# ArtemisFailoverMessageLoss

In a failover configuration, we observed an issue where, after a temporary unavailability of all brokers (e.g., due to a
short network interruption), the consumer failed to properly resume message consumption. Although the brokers became
fully available again, the consumer either received only every second message for a period of time or, in some cases,
stopped receiving messages altogether.

## Setup

- broker/client version: 2.41
- Java 21
  Two broker cluster with no persistence (broker.xml files are provided) configured for high availability.
  In my tests the acceptors and connectors were configured like this.
- Configuration broker hostA:

```
<acceptors>
    <acceptor name="client">tcp://0.0.0.0:6666?protocols=CORE,AMQP;tcpSendBufferSize=1048576;tcpReceiveBufferSize=1048576;useEpoll=true;supportAdvisory=false;suppressInternalManagementObjects=true;ha=true;failoverAttempts=-1</acceptor>
    <acceptor name="cluster">tcp://0.0.0.0:6000?protocols=CORE;tcpSendBufferSize=1048576;tcpReceiveBufferSize=1048576;useEpoll=true</acceptor>
</acceptors>
<connectors>
    <connector name="net-hostA">tcp://hostA:6000</connector>
    <connector name="net-hostB">tcp://hostB:6000</connector>
</connectors>
<cluster-connections>
    <cluster-connection name="test-net">
        <connector-ref>net-hostA</connector-ref>
        <static-connectors>
            <connector-ref>net-hostB</connector-ref>
        </static-connectors>
    </cluster-connection>
</cluster-connections>
```

- Configuration broker hostB:

```
<acceptors>
    <acceptor name="client">tcp://0.0.0.0:6666?protocols=CORE,AMQP;tcpSendBufferSize=1048576;tcpReceiveBufferSize=1048576;useEpoll=true;supportAdvisory=false;suppressInternalManagementObjects=true;ha=true;failoverAttempts=-1</acceptor>
    <acceptor name="cluster">tcp://0.0.0.0:6000?protocols=CORE;tcpSendBufferSize=1048576;tcpReceiveBufferSize=1048576;useEpoll=true</acceptor>
</acceptors>
<connectors>
    <connector name="net-hostA">tcp://hostA:6000</connector>
    <connector name="net-hostB">tcp://hostB:6000</connector>
</connectors>
<cluster-connections>
    <cluster-connection name="test-net">
        <connector-ref>net-hostB</connector-ref>
        <static-connectors>
            <connector-ref>net-hostA</connector-ref>
        </static-connectors>
    </cluster-connection>
</cluster-connections>
```

## Performed Test

- Run SimpleListenerCount.main() on hostC and SimplePublisherCount.main() on hostD.
- The publisher will publish incrementing integers on the topic count.topic and the listener will compare the received
  counter with the last one recieved. It will print if it lost any messages (number is missing)
- cut the connection from hostC (listener) to both brokers (hostA and hostB) while the publisher (hostD) still publishes
  messages
- check the missing messages

## recreate information

- build two jars (jar1 with main=SimpleListener.main() and jar2 with main=SimplePublisher.main())
- setup of brokers and clients as described
- we simulated the connection loss by disabling WLAN on hostC
- Set a broker URL either in your IDE in SimpleListenerCount.brokerUrl and SimplePublisherCount.brokerUrl or as the
  first
  command line argument.
    - In this case I used: (tcp://hostA:6666,tcp://hostB:6666)?failoverAttempts=-1

## expected result

- as we not have persistence enabled we expect message loss while the network is not reachable
- after the brokers are reachable again we expect they reconnect to the brokers and receive messages without message
  loss

## actual result

- message loss while not reachable as expected
- reconnected as expected
- in some cases received only every second message for some time then no more message loss
- in some cases no messages received anymore

# question

- is there an error in my configuration / code?
- did we expect the wrong results?