#include "mysqlhandler.h"

#include <QSqlQuery>
#include <QSqlError>
#include <QTimer>
#include <QCryptographicHash>
#include <QSqlRecord>

#include "user.h"
#include "permission.h"
#include "logger.h"

MySQLHandler * MySQLHandler::m_instance = NULL;

MySQLHandler * MySQLHandler::instance()
{
    if (!m_instance)
        m_instance = new MySQLHandler;
    return m_instance;
}

MySQLHandler::MySQLHandler(QObject *parent)
    : QObject{parent}
{
    m_db = QSqlDatabase::addDatabase("QMYSQL");
    m_db.setHostName("localhost");
    m_db.setDatabaseName("rmtandroid");
    m_db.setUserName("root");
    m_db.setPassword("");

    if (!m_db.open()) {
        Logger::warning() << "Failed to connect:" << m_db.lastError().text();
        return;
    }

    QTimer *timerPingQuery = new QTimer;
    timerPingQuery->setInterval(180 * 1000);
    connect(timerPingQuery, &QTimer::timeout, this, [=]() {
        pingQuery();
    });
    timerPingQuery->start();
}

void MySQLHandler::pingQuery()
{
    if (!m_db.isOpen())
        return;

    QSqlQuery query;
    query.exec("select No from users");
}

int MySQLHandler::singup(const User & user)
{
    int nResult = 0; // 0 success, 1 exist user, 2 unknown error
    QString querySelect = QString("select * from users where phone='%1'").arg(user.phone());
    QSqlQuery query(querySelect);
    if (query.next())
    {
        nResult = 1;
    }
    else
    {
        QByteArray hashPwdByte = QCryptographicHash::hash(user.password().toUtf8(), QCryptographicHash::Md5);
        QString pwdString  = hashPwdByte.toHex();
        QString queryInsertString = QString("insert into users (phone, password, super_id) values ('%1', '%2', '%3');").arg(user.phone(), pwdString, user.superId());
        QSqlQuery queryInsert;
        nResult = queryInsert.exec(queryInsertString) ? 0 : 2;
    }
    return nResult;
}

int MySQLHandler::login(User *user)
{
    int nResult = 0; // 0 success, 1 unregistered phone number, 2 incorrect password, 3 other reason (ex: blocked)
    QString querySelect = QString("select * from users where phone='%1'").arg(user->phone());
    QSqlQuery query(querySelect);
    if (query.next())
    {
        int id = query.record().value(0).toInt();
        QString pwdString = query.record().value(2).toString();
        QString super_id = query.record().value(3).toString();
        int status = query.record().value(4).toInt();
        user->setId(id);
        user->setSuperId(super_id);
        user->setUserStatus((User::UserStatus)status);

        QByteArray hashPwdByte = QCryptographicHash::hash(user->password().toUtf8(), QCryptographicHash::Md5);
        QString pwdMD5String  = hashPwdByte.toHex();
        if (pwdString.compare(pwdMD5String))
        {
            nResult = 2;
        }
        else
        {
            if (status)
            {
                nResult = 3;
            }

            // add code to record login history in future
        }
    }
    else
    {
        nResult = 1;
    }

    return nResult;
}

int MySQLHandler::resetPassword(const QString & phone, const QString & newPwd)
{
    int nResult = 0;
    QByteArray hashPwdByte = QCryptographicHash::hash(newPwd.toUtf8(), QCryptographicHash::Md5);
    QString pwdMD5String  = hashPwdByte.toHex();

    QString updateString = QString("update users set password='%1' where phone='%2'").arg(pwdMD5String, phone);
    QSqlQuery query;
    nResult = query.exec(updateString) ? 0 : 1;
    return nResult;
}

int MySQLHandler::updatePassword(const QString & phone, const QString &curPwd, const QString & newPwd)
{
    int nResult = 0;

    QString selectString = QString("select password from users where phone='%1'").arg(phone);
    QSqlQuery querySelect(selectString);
    if (querySelect.next())
    {
        QString curHashPwdByte = QCryptographicHash::hash(curPwd.toUtf8(), QCryptographicHash::Md5).toHex();
        QString hashOldPwdByte = querySelect.record().value(0).toString();
        if (curHashPwdByte.compare(hashOldPwdByte))
        {
            nResult = 1;
            return nResult;
        }
    }
    else
    {
        nResult = 2;
        return nResult;
    }

    nResult = resetPassword(phone, newPwd) ? 2 : 0;
    return nResult;
}

void MySQLHandler::getPermission(const QString & phone, Permission & permission)
{
    QString selectString = QString("select * from permissions where phone='%1'").arg(phone);
    QSqlQuery query(selectString);
    if (query.next())
    {
        for (int i = 0; i < Permission::PermissionInfoCount; i++)
        {
            bool allowed = query.record().value(i + 1).toBool();
            permission.setAllow((Permission::PermissionInfo)i, allowed);
        }
    }
}

int MySQLHandler::setPermission(const QString & phone, const Permission & permission)
{
    int nResult = 0;
    QString selectString = QString("select * from permissions where phone='%1'").arg(phone);
    QSqlQuery query(selectString);
    if (query.next())
    {
        QString updateString = QString("update permissions set screen=%1, microphone=%2, camera=%3, remote_input=%4, access_file=%5 where phone='%6'")
        .arg(
            QString::number(permission.isAllowed(Permission::Screen)),
            QString::number(permission.isAllowed(Permission::Microphone)),
            QString::number(permission.isAllowed(Permission::Camera)),
            QString::number(permission.isAllowed(Permission::RemoteInput)),
            QString::number(permission.isAllowed(Permission::AccessFile)),
            phone
            );
        QSqlQuery updateQuery;
        nResult = updateQuery.exec(updateString) ? 0 : 1;
    }
    else
    {
        QString insertString = QString("insert into permissions (phone, screen, microphone, camera, remote_input, access_file) values ('%1', %2, %3, %4, %5, %6);")
        .arg(
            phone,
            QString::number(permission.isAllowed(Permission::Screen)),
            QString::number(permission.isAllowed(Permission::Microphone)),
            QString::number(permission.isAllowed(Permission::Camera)),
            QString::number(permission.isAllowed(Permission::RemoteInput)),
            QString::number(permission.isAllowed(Permission::AccessFile))
            );
        QSqlQuery insertQuery;
        nResult = insertQuery.exec(insertString)? 0 : 1;
    }
    return nResult;
}

void MySQLHandler::loadAllUsers(QList<User> & users)
{
    QString selectString = "select No, phone, super_id from users where status = 0";
    QSqlQuery query(selectString);
    while (query.next())
    {
        int ix = 0;
        User user;
        user.setId(query.record().value(ix++).toInt());
        user.setPhone(query.record().value(ix++).toString());
        user.setSuperId(query.record().value(ix++).toString());
        users.append(user);
    }
}
