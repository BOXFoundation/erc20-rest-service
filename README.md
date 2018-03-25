# ERC-20 RESTful service

This application provides a RESTful service for creating and managing 
[ERC-20 tokens](https://github.com/ethereum/EIPs/issues/20). 
It has been built using [Spring Boot](https://projects.spring.io/spring-boot/), and 
[web3j](https://web3j.io).

It works with both [Geth](https://github.com/ethereum/go-ethereum), 
[Parity](https://github.com/paritytech/parity), and 
[Quorum](https://github.com/jpmorganchase/quorum).

For Quorum, the RESTful semantics are identical, with the exception that if you wish to create 
a private transaction, you populate a HTTP header name *privateFor* with a comma-separated
list of public keys


## Build

To build a runnable jar file:

```bash
./gradlew clean build
```

## Run

Using Java 1.8+:

```bash
java -jar build/libs/azure-demo-0.1.jar 
```

By default the application will log to a file named erc20-web3j.log. 

## To run locally
- Run `mysql` and initialize it.
```
mysql> create database db_example; -- Create the new database
mysql> create user 'springuser'@'localhost' identified by 'ThePassword'; -- Creates the user
mysql> grant all on db_example.* to 'springuser'@'localhost'; -- Gives all the privileges to the new user on the newly created database
```
- Run an Ethereum full node/client, for example
```
geth --identity "MyNodeName" --rpc --rpcport "8081" --rpccorsdomain "*" --datadir priv_test --port "30303" --nodiscover --rpcapi "db,eth,net,web3,personal" --networkid 1999
```
- Update `config/application.yml` and `application.properties` under `resources/` to your local settings. For example, 
```
# Endpoint of an Ethereum or Quorum node we wish to use. 
# To use IPC simply provide a file path to the socket, such as /path/to/geth.ipc
nodeEndpoint=http://localhost:22000
# The Ethereum or Quorum address we wish to use when transacting.
# Note - this address must be already unlocked in the client
fromAddress=0xed9d02e382b34818e88b88a309c7fe71e65f419d
```

## Usage

All available application endpoints are documented using [Swagger](http://swagger.io/).

You can view the Swagger UI at http://localhost:8080/swagger-ui.html. From here you
can perform all POST and GET requests easily to facilitate deployment of, transacting 
with, and querying state of ERC-20 tokens.

![alt text](https://github.com/blk-io/erc20-rest-service/raw/master/images/full-swagger-ui.png "Swagger UI screen capture")


## TODOs
- Unlock account before transacting
- Database layer to store user accounts
- Add timestamp & txid in tx history
- HD wallet
- QR code (front end)
- Set tx fee/gas like in Mist
- Sanitize address