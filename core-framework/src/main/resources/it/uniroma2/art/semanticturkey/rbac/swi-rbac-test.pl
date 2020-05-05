
:- consult('rbac_tbox.pl').
:- consult('swi_support.pl').

lr(ROLE) :-
	atomic_list_concat(['roles/role_',ROLE,'.pl'], ROLEFILE),
	consult(ROLEFILE).