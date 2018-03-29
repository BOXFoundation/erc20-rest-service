package fm.castbox.wallet.controller;

import fm.castbox.wallet.dto.AddressDto;
import fm.castbox.wallet.service.EthAccountService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.Optional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Api("Ethereum address management APIs")
@RestController
@Validated
public class EthAddressController {
  private final EthAccountService ethAccountService;

  public EthAddressController(EthAccountService ethAccountService) {
    this.ethAccountService = ethAccountService;
  }

  @ApiOperation(
      value = "Create an Ethereum address for a new user",
      notes = "Returns a new ETH address")
  @RequestMapping(value = "/ethereum/address/create/{userId:[a-zA-Z0-9]{32}}")
  public AddressDto createAddress(@PathVariable("userId") String userId) {
    Optional<String> address = ethAccountService.createAddress(userId);
    if (address.isPresent()) {
      return new AddressDto(address.get());
    } else {
      throw new IllegalStateException("Exception happened when creating a new address");
    }
  }

  @ApiOperation(
      value = "Change to another Ethereum address for an existing user",
      notes = "Returns a new ETH address")
  @RequestMapping(value = "/ethereum/address/change/{userId:[a-zA-Z0-9]{32}}")
  public AddressDto changeAddress(@PathVariable("userId") String userId) {
    Optional<String> address = ethAccountService.changeAddress(userId);
    if (address.isPresent()) {
      return new AddressDto(address.get());
    } else {
      throw new IllegalStateException("Exception happened when changing a new address");
    }
  }
}
