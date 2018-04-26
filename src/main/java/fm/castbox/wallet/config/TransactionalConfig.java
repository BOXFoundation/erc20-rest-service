package fm.castbox.wallet.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;

@Configuration
//@EnableTransactionManagement(mode= AdviceMode.ASPECTJ)
//@EnableAspectJAutoProxy
//@EnableLoadTimeWeaving
public class TransactionalConfig {

  @Bean
  @Autowired
  public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
    JpaTransactionManager txManager = new JpaTransactionManager();
    txManager.setEntityManagerFactory(emf);
    return txManager;
  }

//  @Bean
//  public InstrumentationLoadTimeWeaver loadTimeWeaver() throws Throwable {
//    InstrumentationLoadTimeWeaver loadTimeWeaver = new InstrumentationLoadTimeWeaver();
//    return loadTimeWeaver;
//  }
}
