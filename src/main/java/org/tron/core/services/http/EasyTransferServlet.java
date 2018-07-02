package org.tron.core.services.http;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.EasyTransferMessage;
import org.tron.api.GrpcAPI.EasyTransferResponse;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.common.crypto.ECKey;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.services.http.JsonFormat.ParseException;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;


@Component
@Slf4j
public class EasyTransferServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String input = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
    EasyTransferMessage.Builder build = EasyTransferMessage.newBuilder();
    try {
      JsonFormat.merge(input, build);
    } catch (ParseException e) {
      logger.debug("ParseException: {}", e.getMessage());
    }
    byte[] privateKey = wallet.pass2Key(build.getPassPhrase().toByteArray());
    ECKey ecKey = ECKey.fromPrivate(privateKey);
    byte[] owner = ecKey.getAddress();
    TransferContract.Builder builder = TransferContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setToAddress(build.getToAddress());
    builder.setAmount(build.getAmount());

    TransactionCapsule transactionCapsule = null;
    GrpcAPI.Return.Builder returnBuilder = GrpcAPI.Return.newBuilder();
    EasyTransferResponse.Builder responseBuild = EasyTransferResponse.newBuilder();
    try {
      transactionCapsule = wallet
          .createTransactionCapsule(builder.build(), ContractType.TransferContract);
    } catch (ContractValidateException e) {
      returnBuilder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
          .setMessage(ByteString.copyFromUtf8(e.getMessage()));
      responseBuild.setResult(returnBuilder.build());
      response.getWriter().println(JsonFormat.printToString(responseBuild.build()));
      return;
    }

    transactionCapsule.sign(privateKey);
    GrpcAPI.Return retur = wallet.broadcastTransaction(transactionCapsule.getInstance());
    responseBuild.setTransaction(transactionCapsule.getInstance());
    responseBuild.setResult(retur);
    response.getWriter().println(JsonFormat.printToString(responseBuild.build()));
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    doGet(request, response);
  }
}