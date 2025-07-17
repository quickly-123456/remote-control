#ifndef RDTDEFINE_H
#define RDTDEFINE_H

// RDT means remote Android

#define RDT_PORT    5050

enum RdtSignal
{
    CS_USER = 0xFF,
    SC_USER,

    CS_VUE,
    SC_VUE,

    CS_SCREEN,
    SC_SCREEN,

    CS_USER_DISCONNECT,
    SC_USER_DISCONNECT,

    CS_MOBILE_ADMIN,
    SC_MOBILE_ADMIN,

    CS_ONOFF,
    SC_ONOFF,

    CS_TOUCHED,
    SC_TOUCHED
};

#endif // RDTDEFINE_H
