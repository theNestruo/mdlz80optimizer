; Test case: 
	macro mymacro 2
	repeat @0
	ld a,low @1
	ld (var),a
	or a
    jr nz,1f
	ld a,high @1
	ld (var),a
1:	
	endrepeat
	endmacro

	mymacro 2, #0203

loop:
	jr loop

var:
	db 0
