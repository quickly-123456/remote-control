#ifndef VUESOCKET_H
#define VUESOCKET_H

#include <QObject>

class QWebSocket;
class RDTMessage;

class VueSocket : public QObject
{
    Q_OBJECT
public:
    explicit        VueSocket(const QString & super_id, QWebSocket *socket, QObject *parent = nullptr);

    QWebSocket *    socket() const;
    void            setSocket(QWebSocket * socket);

    QString         superId() const;

    bool            isConnected() const;

    void            send(RDTMessage & msg);

signals:

private:
    QWebSocket *    _socket;

    QString         _super_id;
};

#endif // VueSocket_H
