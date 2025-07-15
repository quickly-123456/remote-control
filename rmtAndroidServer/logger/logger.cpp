#include "logger.h"

#include <QDebug>

Logger::Logger(Logger::LogLevel level) : _level(level){

}

Logger::~Logger() {
    switch (_level) {
    case Info:
        qInfo() << _stream; // Output the accumulated message
        break;
    case Warning:
        qWarning() << _stream;
        break;
    case Critical:
        qCritical() << _stream;
        break;
    }
    printf("%s\n", _stream.toUtf8().constData());
}

Logger Logger::info()
{
    return Logger(Logger::Info);
}

Logger Logger::warning()
{
    return Logger(Logger::Warning);
}

Logger Logger::critical()
{
    return Logger(Logger::Critical);
}

Logger & Logger::operator<<(const QString & msg)
{
    _stream.append(" ");
    _stream.append(msg);
    return *this;
}

Logger & Logger::operator<<(const char * msg)
{
    _stream.append(" ");
    _stream.append(msg);
    return *this;
}

Logger & Logger::operator<<(const QByteArray & msg)
{
    _stream.append(" ");
    _stream.append(msg);
    return *this;
}

Logger & Logger::operator<<(int msg)
{
    _stream.append(" ");
    _stream.append(QString::number(msg));
    return *this;
}

Logger & Logger::operator<<(float msg)
{
    _stream.append(" ");
    _stream.append(QString::number(msg));
    return *this;
}
