; Test case: 
game_version: equ "2"
    add a, 0x0a
    add a, 0x0b
    add a, 0x0c
    db (0x0001 / 65536)
    db (0x0001 / 0x0100) & 0xff
    db (0x0001 % 0x0100)
    dw 0x0000
    db (0x0002 / 0x0100)
    db (0x0002 % 0x0100)
    db 0x0003