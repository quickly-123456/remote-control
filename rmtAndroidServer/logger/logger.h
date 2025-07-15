#ifndef LOGGER_H
#define LOGGER_H

#include <QString>

class Logger {
public:
    // Enumeration for log levels
    enum LogLevel {
        Info,
        Warning,
        Critical
    };

    Logger(LogLevel level = Info);

    Logger& operator<<(const QString & msg);
    Logger& operator<<(const char * msg);
    Logger& operator<<(const QByteArray & msg);
    Logger& operator<<(int msg);
    Logger& operator<<(float msg);

    ~Logger();

    static Logger info();
    static Logger warning();
    static Logger critical();

private:
    LogLevel _level;
    QString _stream; // Using QString to accumulate messages
};


#endif // LOGGER_H
