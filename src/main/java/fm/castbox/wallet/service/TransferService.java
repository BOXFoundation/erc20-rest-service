package fm.castbox.wallet.service;


import fm.castbox.wallet.dto.TransferRDto;
import fm.castbox.wallet.dto.TransferQDto;

public interface TransferService {

//  @Transactional
//  TransferRDto internalTransfer(EthAccount fromAccount, EthAccount toAccount,
//                                       String symbol, BigInteger value, String note) throws Exception;

  TransferRDto<ContractService.TransferEventResponse> transferFromUser(
          String fromUserId, TransferQDto transferQDto) throws Exception;

}
