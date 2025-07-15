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
};

#endif // RDTDEFINE_H
