#include "user.h"

#include <QJsonObject>
#include <QJsonDocument>

#include "rdtmessage.h"

User::User(const QString & phone, const QString & pwd, const QString & super_id)
{
    _phone = phone;
    _password = pwd;
    _super_id = super_id;
    _status = UserStatusNormal;
    _id = -1;
}

int User::id() const
{
    return _id;
}

void User::setId(int id)
{
    _id = id;
}

QString User::phone() const
{
    return _phone;
}

void User::setPhone(const QString & phone)
{
    _phone = phone;
}

QString User::password() const
{
    return _password;
}

void User::setPassword(const QString & password)
{
    _password = password;
}

QString User::superId() const
{
    return _super_id;
}

void User::setSuperId(const QString & super_id)
{
    _super_id = super_id;
}

User::UserStatus User::status() const
{
    return _status;
}

void User::setUserStatus(User::UserStatus st)
{
    _status = st;
}

QString User::toJSON() const
{
    QJsonObject json;
    json["phone"] = _phone;
    json["super_id"] = _super_id;
    json["password"] = _password;
    json["status"] = _status;
    json["id"] = _id;
    QJsonDocument jsonDoc(json);
    QByteArray jsonByte = jsonDoc.toJson(QJsonDocument::JsonFormat::Compact);
    return QString::fromUtf8(jsonByte);
}

void User::fromMessage(RDTMessage & rdtMessage)
{
    rdtMessage >> _phone >> _super_id;
}
