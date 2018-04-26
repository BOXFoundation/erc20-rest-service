package fm.castbox.wallet.service;


import fm.castbox.wallet.dto.TransactionResponse;
import fm.castbox.wallet.dto.TransferQDto;

public interface TransferService {

//  @Transactional
//  TransactionResponse internalTransfer(EthAccount fromAccount, EthAccount toAccount,
//                                       String symbol, BigInteger value, String note) throws Exception;

  TransactionResponse<ContractService.TransferEventResponse> transferFromUser(
          String fromUserId, TransferQDto transferQDto) throws Exception;

}
