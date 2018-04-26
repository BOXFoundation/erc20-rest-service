package fm.castbox.wallet.controller;

import com.alibaba.fastjson.serializer.JSONSerializable;
import fm.castbox.wallet.dto.*;
import fm.castbox.wallet.service.EthService;
import fm.castbox.wallet.util.APISignUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.Optional;
import javax.validation.Valid;

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
  @PostMapping("/0.1/eth/address/create")
  public AddressDto createAddress(@Valid @RequestBody UserIdDto userIdDto) {
    Optional<String> address = ethService.createAddress(userIdDto.getUserId());
    if (address.isPresent()) {
      return new AddressDto(address.get());
    } else {
      throw new IllegalStateException("Exception happened when creating a new address");
    }
  }

  @ApiOperation("Change a new ETH address for an existing user")
  @PutMapping("/0.1/eth/address/change")
  public AddressDto changeAddress(@Valid @RequestBody UserIdDto userIdDto) {
    Optional<String> address = ethService.changeAddress(userIdDto.getUserId());
    if (address.isPresent()) {
      return new AddressDto(address.get());
    } else {
      throw new IllegalStateException("Exception happened when changing a new address");
    }
  }

  @ApiOperation("Get an user's ETH balance")
  @GetMapping("/0.1/eth/balance/{userId:[a-zA-Z0-9]{32}}")
  public BalanceDto balance(@PathVariable("userId") String userId) {
    return ethService.balanceOf(userId);
  }

  @ApiOperation("Estimate fee cost for a transfer intent")
  @PostMapping("/0.1/eth/estimate-transfer-fee")
  public GeneralResponse<EstFeeRDto> estimateTransferFee(@Valid @RequestBody EstFeeQDto estFeeQDto) throws Exception{
    return new GeneralResponse(ethService.estimateTransferFee(estFeeQDto));
  }

}
