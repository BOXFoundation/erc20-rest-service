package fm.castbox.wallet.controller;

import fm.castbox.wallet.dto.AddressDto;
import fm.castbox.wallet.dto.BalanceDto;
import fm.castbox.wallet.service.EthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.Optional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;


@Api("Ethereum related APIs")
@RestController
@Validated
public class EthController {

  private final EthService ethService;

  public EthController(EthService ethService) {
    this.ethService = ethService;
  }

  @ApiOperation(
      value = "Create an Ethereum address for a new user",
      notes = "Returns a new ETH address")
  @PostMapping(value = "/eth/address/create/{userId:[a-zA-Z0-9]{32}}")
  public AddressDto createAddress(@PathVariable("userId") String userId) {
    Optional<String> address = ethService.createAddress(userId);
    if (address.isPresent()) {
      return new AddressDto(address.get());
    } else {
      throw new IllegalStateException("Exception happened when creating a new address");
    }
  }

  @ApiOperation(
      value = "Change to another Ethereum address for an existing user",
      notes = "Returns a new ETH address")
  @PutMapping(value = "/eth/address/change/{userId:[a-zA-Z0-9]{32}}")
  public AddressDto changeAddress(@PathVariable("userId") String userId) {
    Optional<String> address = ethService.changeAddress(userId);
    if (address.isPresent()) {
      return new AddressDto(address.get());
    } else {
      throw new IllegalStateException("Exception happened when changing a new address");
    }
  }

  @ApiOperation(value = "Get an user's ETH balance")
  @GetMapping(value = "/eth/balance/{userId:[a-zA-Z0-9]{32}}")
  public BalanceDto balance(@PathVariable("userId") String userId) {
    return ethService.balanceOf(userId);
  }
}
