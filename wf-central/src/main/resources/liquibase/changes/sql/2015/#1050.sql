UPDATE HistoryEvent_Service SET nID_StatusType = 0 WHERE sID_Status = '������ ������';
UPDATE HistoryEvent_Service SET nID_StatusType = 11 WHERE sID_Status = '������ ��������';
UPDATE HistoryEvent_Service SET nID_StatusType = 2 WHERE sID_Status LIKE '%(� �����)';
UPDATE HistoryEvent_Service SET nID_StatusType = 3 WHERE sID_Status = '����� �� ��������� �����';
UPDATE HistoryEvent_Service SET sID_Status = regexp_replace(sID_Status, '(� �����)$', '') WHERE sID_Status LIKE '%(� �����)';
UPDATE HistoryEvent_Service SET sID_Status = '' WHERE sID_Status LIKE '%������ ������%' OR sID_Status LIKE '%������ ��������%' OR sID_Status LIKE '%����� �� ��������� �����%';
UPDATE HistoryEvent_Service SET sUserTaskName = sID_Status; 
