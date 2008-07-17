package.path = "?.lua;src/test/errors/?.lua"
require 'args'

-- arg type tests for coroutine library functions

-- coroutine.create
banner('coroutine.create')
checkallpass('coroutine.create',{somefunction})
checkallerrors('coroutine.create',{notafunction},'bad argument')

-- coroutine.resume
banner('coroutine.resume')
local co = coroutine.create(function() while true do coroutine.yield() end end)
checkallpass('coroutine.resume',{{co},anylua})
checkallerrors('coroutine.resume',{notathread},'bad argument #1')

-- coroutine.running
banner('coroutine.running')
checkallpass('coroutine.running',{anylua})

-- coroutine.status
banner('coroutine.status')
checkallpass('coroutine.status',{{co}})
checkallerrors('coroutine.status',{notathread},'bad argument #1')

-- coroutine.wrap
banner('coroutine.wrap')
checkallpass('coroutine.wrap',{somefunction})
checkallerrors('coroutine.wrap',{notafunction},'bad argument #1')

-- coroutine.yield
banner('coroutine.yield')
local function f() 
	print( 'yield', coroutine.yield() )
	print( 'yield', coroutine.yield(astring) )
	print( 'yield', coroutine.yield(anumber) )
	print( 'yield', coroutine.yield(aboolean) )
end
local co = coroutine.create( f )
repeat 
	print( 'resume', coroutine.resume(co,astring,anumber) )
until coroutine.status(co) ~= 'suspended'

