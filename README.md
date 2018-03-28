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


## To run locally
- Run `mysql` and initialize it.
```
mysql> create database db_example; -- Create the new database
mysql> create user 'springuser'@'localhost' identified by 'ThePassword'; -- Creates the user
mysql> grant all on db_example.* to 'springuser'@'localhost'; -- Gives all the privileges to the new user on the newly created database
```

- Run an Ethereum full node/client and start a private testnet, for example
```bash
// init with genesis block
> geth --identity "MyNodeName" --rpc --rpcport "8081" --rpccorsdomain "*" --datadir priv_test --port "30303" --nodiscover --rpcapi "db,eth,net,web3,personal" --networkid 1999 init path/to/fm/castbox/wallet/config/CustomGenesis.json
// launch geth
> geth --identity "MyNodeName" --rpc --rpcport "8081" --rpccorsdomain "*" --datadir priv_test --port "30303" --nodiscover --rpcapi "db,eth,net,web3,personal" --networkid 1999
```
Copy key file `fm/castbox/wallet/config/UTC--2018-03-13T18-27-23.961533000Z--f6de496ec5601d74937ddd77af09c8cd4ba41ab5` to datadir `priv_test/`. This is needed to unlock our main wallet account to transact.

- Launch wallet server
Update `config/application.yml` and `application.properties` under `resources/` to your local settings (e.g., mysql port number) if they differ from default settings.
```bash
./gradlew bootRun
```

## Usage

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
1. Other APIs for wallet integration
- `GET /{userId}/address`: Get user's address
- `GET /{contractAddress}/balanceOf/{userId}`: Get token balance of a user
- `POST /{contractAddress}/transferFrom`: Transfer a user's tokens to an address
- `GET /{contractAddress}/listtx/{userId}`: Returns a list of token transactions for a given user