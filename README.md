# Wallet RESTful APIs

This application provides a RESTful service for creating and managing [ERC-20 tokens](https://github.com/ethereum/EIPs/issues/20). It has been built using [Spring Boot](https://projects.spring.io/spring-boot/), and [web3j](https://web3j.io).

It works with both [Geth](https://github.com/ethereum/go-ethereum), [Parity](https://github.com/paritytech/parity), and [Quorum](https://github.com/jpmorganchase/quorum).

For Quorum, the RESTful semantics are identical, with the exception that if you wish to create a private transaction, you populate a HTTP header name *privateFor* with a comma-separated list of public keys


## How to Run Unit Tests

### Launch a MySQL instance

    rm -rf ~/docker_data/contentbox-mysql
    mkdir -p ~/docker_data/contentbox-mysql
    docker run --name contentbox-mysql -v ~/docker_data/contentbox-mysql:/var/lib/mysql -e MYSQL_USER=contentbox -e MYSQL_PASSWORD=ThePassw0rd -e MYSQL_DATABASE=wallet -e MYSQL_ALLOW_EMPTY_PASSWORD=yes -p 3306:3306 -d mysql

The command above will launch a MySQL instance, create a table `wallet`, add a user `contentbox` and set its password automatically. Make sure the username and password are identical with `src/main/resources/application.yml`.

### Create an Ethereum private network

First, install `geth` on your Macbook,

    # https://www.ethereum.org/cli
    brew tap ethereum/ethereum
    brew install ethereum
    brew install solidity

Second, create a genesis block,

    rm -rf ~/private_ethereum
    mkdir ~/private_ethereum
    geth --identity "MyNodeName" --rpc --rpcport "8081" --rpccorsdomain "*" --datadir ~/private_ethereum --port "30303" --nodiscover --rpcapi "db,eth,net,web3,personal" --networkid 1999 init /path/to/src/main/resources/CustomGenesis.json

Third, start the private network,

    geth --identity "MyNodeName" --rpc --rpcport "8081" --rpccorsdomain "*" --datadir ~/private_ethereum --port "30303" --nodiscover --rpcapi "db,eth,net,web3,personal" --networkid 1999 console

Last, copy the wallet file `/path/to/src/main/resources/f6de496ec5601d74937ddd77af09c8cd4ba41ab5.json` to `~/private_ethereum/keystore`. This file is needed to unlock the main account, which is used in unit tests.

### Run all unit tests

    ./gradlew test

## API References

All available application endpoints are documented using [Swagger](http://swagger.io/).

You can view the Swagger UI at http://localhost:8080/swagger-ui.html. From here you
can perform all POST and GET requests easily to facilitate deployment of, transacting 
with, and querying state of ERC-20 tokens.

![alt text](https://github.com/blk-io/erc20-rest-service/raw/master/images/full-swagger-ui.png "Swagger UI screen capture")

1. Deploy our ERC-20 token contract

    `POST /deploy`
    ```
    {
      "decimalUnits": 0,
      "initialAmount": 1000,
      "tokenName": "BOX token",
      "tokenSymbol": "BOX"
    }
    ```
    Upon successful deployment, the contract address will be returned, which can be used to interact with the contract.
1. Subscribe to token transfer events of a specific contract address
`POST /subscribe/{contractAddress}`
1. Get some initial coins for users
    `GET /{userId}/address`: Get user's address. Generate a few users and change their balances to positive in mysql directly so they can start transacting.
1. Other APIs for wallet integration
- `GET /{contractAddress}/balanceOf/{userId}`: Get token balance of a user
- `POST /{contractAddress}/transferFrom`: Transfer a user's tokens to an address
- `GET /{contractAddress}/listtx/{userId}`: Returns a list of token transactions for a given user