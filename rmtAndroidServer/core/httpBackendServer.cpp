#include "httpBackendServer.h"

#include <QDateTime>
#include <QJsonObject>
#include <QJsonDocument>
#include <QJsonArray>
#include <QList>
#include <QImage>
#include <QDateTime>
#include <QDir>
#include <QNetworkAddressEntry>

#include "appDefine.h"
#include "mysqlhandler.h"
#include "user.h"
#include "permission.h"
#include "logger.h"

#include "rdtserver.h"

HttpBackendServer::HttpBackendServer(QObject *parent) : QTcpServer(parent) {
    if (!listen(QHostAddress::Any, BACKEND_PORT)) {
        Logger::info() << DEBUG_MSG_BACKEND_FAILED << errorString();
    } else {
        Logger::info() << DEBUG_MSG_BACKEND_STARTED_PORT << serverPort();
    }
}

void HttpBackendServer::incomingConnection(qintptr socketDescriptor) {
    QTcpSocket *socket = new QTcpSocket(this);
    socket->setSocketDescriptor(socketDescriptor);

    struct HttpRequestBuffer {
        QByteArray buffer;
        int contentLength = -1;
        bool headersParsed = false;
    };
    auto *req = new HttpRequestBuffer();

    connect(socket, &QTcpSocket::readyRead, this, [=]() {
        req->buffer.append(socket->readAll());

        // Step 1: Parse headers if not already parsed
        if (!req->headersParsed) {
            int headerEnd = req->buffer.indexOf("\r\n\r\n");
            if (headerEnd != -1) {
                req->headersParsed = true;
                QByteArray headers = req->buffer.left(headerEnd);
                QList<QByteArray> lines = headers.split('\n');

                for (int i = 0; i < lines.size(); i++) {
                    const QByteArray & line = lines.at(i);
                    if (line.toLower().startsWith("content-length:")) {
                        req->contentLength = line.mid(15).trimmed().toInt();
                        break;
                    }
                }
            } else {
                return; // Wait for more data
            }
        }

        // Step 2: Check if body is fully received
        int bodyStart = req->buffer.indexOf("\r\n\r\n") + 4;
        int totalNeeded = bodyStart + req->contentLength;
        if (req->contentLength != -1 && req->buffer.size() < totalNeeded) {
            return; // Wait for more body data
        }

        // ✅ Full request is now available
        QByteArray request = req->buffer;
        delete req;

        if (isOption(request)) {
            sendConfirmOption(socket);
            return;
        }

        int isGet = 0;
        API_INFO apiInfo = getApiInfo(request, &isGet);
        procApi(socket, apiInfo, request, isGet);
    });

    connect(socket, &QTcpSocket::disconnected, socket, &QTcpSocket::deleteLater);
}

void HttpBackendServer::procApi(QTcpSocket *socket, API_INFO api, const QByteArray & request, int isGet)
{
    int bodyStart = request.indexOf("\r\n\r\n");
    QByteArray body = request.mid(bodyStart);
    QByteArray httpResponse;

    QJsonDocument jsonDoc = QJsonDocument::fromJson(body);
    if (jsonDoc.isNull() && api != API_INFO_WELCOME) {
        makeWrongFormatResponse(httpResponse);
        socket->write(httpResponse);
        socket->flush();
        socket->disconnectFromHost();
        return;
    }

    const char *api_name[] = API_ALL;
    QString timestamp = QDateTime::currentDateTime().toString("yyyyMMdd hh:mm:ss");
    Logger::info() << timestamp << api_name[api] << jsonDoc.toJson(QJsonDocument::Compact);

    QJsonObject jsonObj = jsonDoc.object();

    switch(api)
    {
    case API_INFO_WELCOME:
    {
        QJsonObject jsonResponse;
        jsonResponse["result"] = 0;
        QString message = QString("欢迎！手机远程控制系统 %1").arg(APP_VERSION);
        jsonResponse["message"] = message;
        QJsonDocument responseDoc(jsonResponse);
        QByteArray response = responseDoc.toJson();
        makeGetResponse(httpResponse, response);
    }
    break;
    case API_INFO_REGISTER:
    {
        if (isGet)
            break;

        QString phone = jsonObj["phone"].toString();
        QString pwd = jsonObj["password"].toString();
        QString super_id = jsonObj["super_id"].toString();

        User user(phone, pwd, super_id);
        int nResult = MySQLHandler::instance()->singup(user);

        QJsonObject jsonResponse;
        jsonResponse["result"] = nResult;
        const char * errMsg[] = MSG_SIGNUP_FAILED_REASON_CH;
        jsonResponse["message"] = nResult ? errMsg[nResult] : MSG_SIGNUP_SUCCESS_CH;
        QJsonDocument responseDoc(jsonResponse);
        QByteArray response = responseDoc.toJson();
        makeGetResponse(httpResponse, response);

        RDTServer::instance()->checkChannelAndUser(user);
    }
    break;
    case API_INFO_LOGIN:
    {
        if (isGet)
            break;

        QString phone = jsonObj["phone"].toString();
        QString pwd = jsonObj["password"].toString();

        User user(phone, pwd);
        int nResult = MySQLHandler::instance()->login(&user);

        QJsonObject jsonResponse;
        jsonResponse["result"] = nResult;
        const char * errMsg[] = MSG_LOGIN_FAILED_REASON_CH;
        jsonResponse["message"] = nResult ? errMsg[nResult] : MSG_LOGIN_SUCCESS_CH;
        jsonResponse["user"] = user.toJSON();
        QJsonDocument responseDoc(jsonResponse);
        QByteArray response = responseDoc.toJson();
        makeGetResponse(httpResponse, response);
    }
    break;
    case API_INFO_RESET_PASSWORD:
    {
        if (isGet)
            break;

        QString phone = jsonObj["phone"].toString();
        QString pwd = jsonObj["new_password"].toString();

        int nResult = MySQLHandler::instance()->resetPassword(phone, pwd);

        QJsonObject jsonResponse;
        jsonResponse["result"] = nResult;
        jsonResponse["message"] = nResult ? MSG_OPERATION_FAILED_CH : MSG_RESET_PWD_SUCCESS_CH;
        QJsonDocument responseDoc(jsonResponse);
        QByteArray response = responseDoc.toJson();
        makeGetResponse(httpResponse, response);
    }
    break;
    case API_INFO_UPDATE_PASSWORD:
    {
        if (isGet)
            break;

        QString phone = jsonObj["phone"].toString();
        QString old_pwd = jsonObj["old_password"].toString();
        QString pwd = jsonObj["new_password"].toString();

        int nResult = MySQLHandler::instance()->updatePassword(phone, old_pwd, pwd);

        const char * errMsg[] = MSG_UPDATE_PWD_FAILED_REASON_CH;
        QJsonObject jsonResponse;
        jsonResponse["result"] = nResult;
        jsonResponse["message"] = nResult ? MSG_OPERATION_FAILED_CH : errMsg[nResult];
        QJsonDocument responseDoc(jsonResponse);
        QByteArray response = responseDoc.toJson();
        makeGetResponse(httpResponse, response);
    }
    break;
    case API_INFO_GET_PERMISSIONS:
    {
        if (isGet)
            break;

        QString phone = jsonObj["phone"].toString();

        Permission permissions;
        MySQLHandler::instance()->getPermission(phone, permissions);

        QJsonObject jsonResponse;
        jsonResponse["result"] = 0;
        jsonResponse["message"] = "";
        jsonResponse["permissions"] = permissions.toJSON();
        QJsonDocument responseDoc(jsonResponse);
        QByteArray response = responseDoc.toJson();
        makeGetResponse(httpResponse, response);
    }
    break;
    case API_INFO_SET_PERMISSIONS:
    {
        if (isGet)
            break;

        QString phone = jsonObj["phone"].toString();

        QString permissionsString = jsonObj["permissions"].toString();
        QJsonDocument permissionsDoc = QJsonDocument::fromJson(permissionsString.toUtf8());

        if (permissionsDoc.isNull() || !permissionsDoc.isObject()) {
            makeWrongFormatResponse(httpResponse);
            return;
        }

        QJsonObject jsonObjPermissions = permissionsDoc.object();
        Permission permissions;
        permissions.fromJSON(jsonObjPermissions);
        int nResult = MySQLHandler::instance()->setPermission(phone, permissions);

        QJsonObject jsonResponse;
        jsonResponse["result"] = nResult;
        jsonResponse["message"] = "";
        jsonResponse["permissions"] = permissions.toJSON();
        QJsonDocument responseDoc(jsonResponse);
        QByteArray response = responseDoc.toJson();
        makeGetResponse(httpResponse, response);
    }
    break;
    default:
        break;
    }

    if  (!httpResponse.length())
        return;

    socket->write(httpResponse);
    socket->flush();
    socket->disconnectFromHost();

}
void HttpBackendServer::makeGetResponse(QByteArray & httpResponse, const QByteArray & jsonResponse)
{
    httpResponse.append("HTTP/1.1 200 OK\r\n");
    httpResponse.append("Content-Type: application/json; charset=utf-8\r\n");
    httpResponse.append("Access-Control-Allow-Origin: *\r\n");
    httpResponse.append("Content-Length: " + QByteArray::number(jsonResponse.size()) + "\r\n");
    httpResponse.append("Connection: close\r\n\r\n");
    httpResponse.append(jsonResponse);
}

void HttpBackendServer::makeWrongFormatResponse(QByteArray & httpResponse)
{
    QJsonObject jsonResponse;
    jsonResponse["result"] = -1;
    jsonResponse["message"] = MSG_INPUT_INCORRECT_CH;
    QJsonDocument responseDoc(jsonResponse);
    QByteArray response = responseDoc.toJson();
    makeGetResponse(httpResponse, response);
}

void HttpBackendServer::sendErrorResponse(QTcpSocket *socket) {
    QByteArray errorResponse = "HTTP/1.1 400 Bad Request\r\n"
                               "Content-Type: application/json; charset=utf-8\r\n"
                               "Connection: close\r\n\r\n"
                               "{\"message\": \"Invalid JSON\"}";

    socket->write(errorResponse);
    socket->disconnectFromHost();
}

bool HttpBackendServer::isOption(const QByteArray & request, const char * api)
{
    bool result = false;
    if (api)
    {
        char szOption[256] = {0,};
        sprintf(szOption, "OPTIONS %s", api);
        if (request.startsWith(szOption))
            result = true;
    }
    else
    {
        const char * apis[] = API_ALL;
        for (int i = 0; i < API_INFO_COUNT; i++)
        {
            char szOption[256] = {0,};
            sprintf(szOption, "OPTIONS %s", apis[i]);
            if (request.startsWith(szOption))
            {
                result = true;
                break;
            }
        }
    }
    return result;
}

API_INFO HttpBackendServer::getApiInfo(const QByteArray & request, int * isGet)
{
    API_INFO result = API_INFO_UNKNOWN;
    const char * apis[] = API_ALL;
    for (int i = 0; i < API_INFO_COUNT; i++)
    {
        char szGetOption[256] = {0,};
        char szPostOption[256] = {0,};
        sprintf(szGetOption, "GET %s", apis[i]);
        sprintf(szPostOption, "POST %s", apis[i]);
        if (request.startsWith(szGetOption))
        {
            result = (API_INFO)i;
            *isGet = 1;
            break;
        }
        else if (request.startsWith(szPostOption))
        {
            result = (API_INFO)i;
            *isGet = 0;
            break;
        }
    }
    return result;
}

void HttpBackendServer::sendConfirmOption(QTcpSocket *socket)
{
    QByteArray response =
        "HTTP/1.1 204 No Content\r\n"
        "Access-Control-Allow-Origin: *\r\n"
        "Access-Control-Allow-Methods: POST, GET, OPTIONS\r\n"
        "Access-Control-Allow-Headers: Content-Type\r\n"
        "Content-Length: 0\r\n"
        "\r\n";
    socket->write(response);
    socket->flush();
    socket->disconnectFromHost();
}
