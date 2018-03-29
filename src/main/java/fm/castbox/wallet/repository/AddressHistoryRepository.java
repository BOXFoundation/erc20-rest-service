package fm.castbox.wallet.repository;


import fm.castbox.wallet.domain.AddressHistory;
import org.springframework.data.repository.CrudRepository;

public interface AddressHistoryRepository extends CrudRepository<AddressHistory, Long> {
  AddressHistory save(AddressHistory addressHistory);
}
