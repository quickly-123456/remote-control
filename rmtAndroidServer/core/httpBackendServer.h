#pragma once

#include <QTcpServer>
#include <QTcpSocket>

#include "backendDefine.h"

class HttpBackendServer : public QTcpServer {
    Q_OBJECT

public:
    explicit    HttpBackendServer(QObject *parent = nullptr);

    static
    QString     getLocalIPAddress();

protected:
    void        incomingConnection(qintptr socketDescriptor) override;

    void        sendErrorResponse(QTcpSocket *socket) ;

    bool        isOption(const QByteArray & request, const char * api = NULL);
    void        sendConfirmOption(QTcpSocket *socket);

    API_INFO    getApiInfo(const QByteArray & request, int * isGet);

    void        procApi(QTcpSocket *socket, API_INFO api, const QByteArray & request, int isGet);

    void        makeGetResponse(QByteArray & httpResponse, const QByteArray & jsonResponse);
    void        makeWrongFormatResponse(QByteArray & httpResponse);

};
