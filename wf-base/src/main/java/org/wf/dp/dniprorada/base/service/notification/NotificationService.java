package org.wf.dp.dniprorada.base.service.notification;

import org.apache.commons.mail.EmailException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wf.dp.dniprorada.util.GeneralConfig;
import org.wf.dp.dniprorada.util.Mail;

/**
 * User: goodg_000
 * Date: 25.08.2015
 * Time: 22:54
 */
public class NotificationService {

   @Autowired
   GeneralConfig generalConfig;

   @Autowired
   Mail mail;

   public void sendTaskCreatedInfoEmail(String receiverEmail, Long nID_Protected) throws EmailException {
      String sHead = String.format("�� ������ ����� �%s �� ������� ����� ������ %s", nID_Protected,
              generalConfig.sHostCentral());

      String sBody = String.format("�� ������ ����� �%s �� ������� ����� ������ %s", nID_Protected,
              generalConfig.sHostCentral()) +
              "<br>(�� ������ ������ ���������� �� ������ �� ������ � ����� \"�������\")" +
              "<br>" +
              "��� ��������� ���� ������ � ������� ��������� - ��� ���� ��������� ���������� ������������ ���� - ����������.<br>";
      mail.reset();

      mail._To(receiverEmail)._Head(sHead)._Body(sBody);

      mail.send();
   }
}
