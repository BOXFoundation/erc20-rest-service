package fm.castbox.wallet.controller;

import fm.castbox.wallet.dto.AddressDto;
import fm.castbox.wallet.dto.BalanceDto;
import fm.castbox.wallet.dto.EstFeeResponse;
import fm.castbox.wallet.dto.UserIdDto;
import fm.castbox.wallet.service.EthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.Optional;
import javax.validation.Valid;

import lombok.Data;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Api("Ethereum related APIs")
@RestController
@Validated
public class EthController {

  private final EthService ethService;

  public EthController(EthService ethService) {
    this.ethService = ethService;
  }

  @ApiOperation("Create an ETH address for a new user")
  @PostMapping("/eth/address/create")
  public AddressDto createAddress(@Valid @RequestBody UserIdDto userIdDto) {
    Optional<String> address = ethService.createAddress(userIdDto.getUserId());
    if (address.isPresent()) {
      return new AddressDto(address.get());
    } else {
      throw new IllegalStateException("Exception happened when creating a new address");
    }
  }

  @ApiOperation("Change a new ETH address for an existing user")
  @PutMapping("/eth/address/change")
  public AddressDto changeAddress(@Valid @RequestBody UserIdDto userIdDto) {
    Optional<String> address = ethService.changeAddress(userIdDto.getUserId());
    if (address.isPresent()) {
      return new AddressDto(address.get());
    } else {
      throw new IllegalStateException("Exception happened when changing a new address");
    }
  }

  @ApiOperation("Get an user's ETH balance")
  @GetMapping("/eth/balance/{userId:[a-zA-Z0-9]{32}}")
  public BalanceDto balance(@PathVariable("userId") String userId) {
    return ethService.balanceOf(userId);
  }

  @ApiOperation("Estimate fee cost for a transfer intent")
  @PostMapping("/eth/estimate-transfer-fee")
  public EstFeeResponse estimateTransferFee(@RequestBody estFeeRequest estFeeRequest){
    return ethService.estimateTransferFee(estFeeRequest.getSymbol(), estFeeRequest.getAmount());
  }

  @Data
  static class estFeeRequest {
    private final String symbol;
    private final String amount;
  }

}
