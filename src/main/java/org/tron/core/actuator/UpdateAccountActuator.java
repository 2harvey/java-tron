package org.tron.core.actuator;

import com.google.common.base.Preconditions;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.db.AccountStore;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract.AccountUpdateContract;
import org.tron.protos.Protocol.Transaction.Result.code;

@Slf4j
public class UpdateAccountActuator extends AbstractActuator {

  AccountUpdateContract accountUpdateContract;
  byte[] accountName;
  byte[] ownerAddress;
  long fee;

  UpdateAccountActuator(Any contract, Manager dbManager) {
    super(contract, dbManager);
    try {
      accountUpdateContract = contract.unpack(AccountUpdateContract.class);
      accountName = accountUpdateContract.getAccountName().toByteArray();
      ownerAddress = accountUpdateContract.getOwnerAddress().toByteArray();
      fee = calcFee();
    } catch (InvalidProtocolBufferException e) {
      logger.error(e.getMessage(), e);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
  }

  @Override
  public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
    try {
      AccountStore accountStore = dbManager.getAccountStore();
      AccountCapsule account = accountStore.get(ownerAddress);

      account.setAccountName(accountName);
      accountStore.put(ownerAddress, account);
      ret.setStatus(fee, code.SUCESS);
    } catch (Exception e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    try {
      if (this.dbManager == null) {
        throw new ContractValidateException("No dbManager!");
      }
      if (accountUpdateContract == null) {
        throw new ContractValidateException(
            "contract type error,expected type [AccountUpdateContract],real type[" + contract
                .getClass() + "]");
      }

      //ToDo check accountName
      Preconditions.checkNotNull(accountName, "AccountName is null");
      if (!Wallet.addressValid(ownerAddress)) {
        throw new ContractValidateException("Invalidate ownerAddress");
      }

      if (!dbManager.getAccountStore().has(ownerAddress)) {
        throw new ContractValidateException("Account has not existed");
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new ContractValidateException(ex.getMessage());
    }
    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return contract.unpack(AccountUpdateContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }
}
