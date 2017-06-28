/*******************************
 ** swi-prolog implementation **
 *******************************/

char_subset(CRUDVRequest, CRUDV) :-
	string_chars(CRUDVRequest,CRUDVRequestList),
	string_chars(CRUDV,CRUDVList),
	subset(CRUDVRequestList, CRUDVList).
	



