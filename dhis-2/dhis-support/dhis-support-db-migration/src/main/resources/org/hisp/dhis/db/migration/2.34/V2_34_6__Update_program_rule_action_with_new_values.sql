update programruleaction set actiontype = 'SHOWWARNING', evaluationtime = 'ON_COMPLETE' where actiontype = 'WARNINGONCOMPLETE';
update programruleaction set actiontype = 'SHOWERROR', evaluationtime = 'ON_COMPLETE' where actiontype = 'ERRORONCOMPLETE';

