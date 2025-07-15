#ifndef USER_H
#define USER_H

#include <QString>

class RDTMessage;
class User
{
public:
    enum UserStatus
    {
        UserStatusNormal,
        UserStatusBlocked,
        UserStatusDeleted,
        UserStatusCount
    };

    User(const QString & phone = "", const QString & pwd = "", const QString & super_id = "");

    int     id() const;
    void    setId(int id);

    QString phone() const;
    void    setPhone(const QString &);

    QString password() const;
    void    setPassword(const QString &);

    QString superId() const;
    void    setSuperId(const QString &);

    UserStatus status() const;
    void setUserStatus(UserStatus st);

    QString toJSON() const;

    void fromMessage(RDTMessage &);

private:
    int     _id;
    QString _phone;
    QString _password;
    QString _super_id;
    UserStatus _status;
};

#endif // USER_H
