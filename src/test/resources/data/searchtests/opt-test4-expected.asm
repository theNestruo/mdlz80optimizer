    org #4000
    ld d, #0c
    ld hl, #c004
    ld e, l
    ld (#d001), a  ; two instructions in the middle, just so that "ld a, 4" and "ld e, 4"
    ld (53250), a
    ld a, e
    ld (#d003), a
    inc de
    ld (#d004), a
    ldir
    ld (53253), a
loop:
    jr loop