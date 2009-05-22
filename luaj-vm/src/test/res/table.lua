local t = { "one", "two", "three", a='aaa', b='bbb', c='ccc' }
table.insert(t,'six'); 
table.insert(t,1,'seven');
table.insert(t,4,'eight');
table.insert(t,7,'nine');
table.insert(t,10,'ten');  print( #t )

-- concat
print( '-- concat tests' )
function tryconcat(t)
	print( table.concat(t) )
	print( table.concat(t,'--') )
	print( table.concat(t,',',2) )
	print( table.concat(t,',',2,2) )
	print( table.concat(t,',',5,2) )
end
tryconcat( { "one", "two", "three", a='aaa', b='bbb', c='ccc' } )
tryconcat( { "one", "two", "three", "four", "five" } )
function tryconcat(t)
	print( table.concat(t) )
	print( table.concat(t,'--') )
	print( table.concat(t,',',2) )
end
tryconcat( { a='aaa', b='bbb', c='ccc', d='ddd', e='eee' } )
tryconcat( { [501]="one", [502]="two", [503]="three", [504]="four", [505]="five" } )
tryconcat( {} )

-- print the elements of a table in a platform-independent way
function eles(t,f)
	f = f or pairs
	all = {}
	for k,v in f(t) do
		table.insert( all, "["..tostring(k).."]="..tostring(v) )
	end
	table.sort( all )
	return "{"..table.concat(all,',').."}"
end

-- insert, maxn
print( '-- insert, len tests' )
local t = { "one", "two", "three", a='aaa', b='bbb', c='ccc' }
print( eles(t), #t )
table.insert(t,'six'); print( eles(t), #t )
table.insert(t,1,'seven'); print( eles(t), #t )
table.insert(t,4,'eight'); print( eles(t), #t )
table.insert(t,7,'nine');  print( eles(t), #t )
table.insert(t,10,'ten');  print( eles(t), #t )
print( '#{}', #{} )
print( '#{"a"}', #{"a"} )
print( '#{"a","b"}', #{"a","b"} )
print( '#{"a",nil}', #{"a",nil} )
print( '#{nil,"b"}', #{nil,"b"} )
print( '#{nil,nil}', #{nil,nil} )
print( '#{"a","b","c"}', #{"a","b","c"} )
print( '#{nil,"b","c"}', #{nil,"b","c"} )
print( '#{"a",nil,"c"}', #{"a",nil,"c"} )
print( '#{"a","b",nil}', #{"a","b",nil} )
print( '#{nil,"b",nil}', #{nil,"b",nil} )
print( '#{nil,nil,"c"}', #{nil,nil,"c"} )
print( '#{"a",nil,nil}', #{"a",nil,nil} )
print( '#{nil,nil,nil}', #{nil,nil,nil} )

-- remove
print( '-- remove tests' )
t = { "one", "two", "three", "four", "five", "six", "seven", [10]="ten", a='aaa', b='bbb', c='ccc' }
print( eles(t), #t )
print( 'table.remove(t)', table.remove(t) ); print( eles(t), #t )
print( 'table.remove(t,1)', table.remove(t,1) ); print( eles(t), #t )
print( 'table.remove(t,3)', table.remove(t,3) ); print( eles(t), #t )
print( 'table.remove(t,5)', table.remove(t,5) ); print( eles(t), #t )
print( 'table.remove(t,10)', table.remove(t,10) ); print( eles(t), #t )
print( 'table.remove(t,-1)', table.remove(t,-1) ); print( eles(t), #t )
print( 'table.remove(t,-1)', table.remove(t,-1) ) ; print( eles(t), #t )

-- sort
print( '-- sort tests' )
function sorttest(t,f)
	t = (t)
	print( table.concat(t,'-') )
	if f then
		table.sort(t,f)
	else	
		table.sort(t)
	end
	print( table.concat(t,'-') )
end	
sorttest{ "one", "two", "three" }
sorttest{  "www", "vvv", "uuu", "ttt", "sss", "zzz", "yyy", "xxx" }
sorttest( {  "www", "vvv", "uuu", "ttt", "sss", "zzz", "yyy", "xxx" }, function(a,b) return b<a end)

-- getn
t0 = {}
t1 = { 'one', 'two', 'three' }
t2 = { a='aa', b='bb', c='cc' }
t3 = { 'one', 'two', 'three', a='aa', b='bb', c='cc' }
print( 'getn('..eles(t0)..')', pcall( table.getn, t0 ) ) 
print( 'getn('..eles(t1)..')', pcall( table.getn, t1 ) ) 
print( 'getn('..eles(t2)..')', pcall( table.getn, t2 ) ) 
print( 'getn('..eles(t3)..')', pcall( table.getn, t3 ) ) 

-- foreach
function test( f, t, result, name ) 
	status, value = pcall( f, t, function(...) 
		print('  -- ',...)
		print('  next',next(t,(...)))
		return result 
	end )
	print( name, 's,v', status, value )
end
function testall( f, t, name ) 
	test( f, t, nil, name..'nil' )
	test( f, t, false, name..'fls' )
	test( f, t, 100, name..'100' )
end
testall( table.foreach, t0, 'table.foreach('..eles(t0)..')' )
testall( table.foreach, t1, 'table.foreach('..eles(t1)..')' )
testall( table.foreach, t2, 'table.foreach('..eles(t2)..')' )
testall( table.foreach, t3, 'table.foreach('..eles(t3)..')' )
testall( table.foreachi, t0, 'table.foreachi('..eles(t0)..')' )
testall( table.foreachi, t1, 'table.foreachi('..eles(t1)..')' )
testall( table.foreachi, t2, 'table.foreachi('..eles(t2)..')' )
testall( table.foreachi, t3, 'table.foreachi('..eles(t3)..')' )

-- pairs, ipairs
function testpairs(f, t, name)
	print( name )
	for a,b in f(t) do
		print( ' ', a, b )
	end
end
function testbothpairs(t)
	testpairs( pairs, t, 'pairs( '..eles(t)..' )' )
	testpairs( ipairs, t, 'ipairs( '..eles(t)..' )' )
end
for i,t in ipairs({t0,t1,t2,t3}) do
	testbothpairs(t)
end
t = { 'one', 'two', 'three', 'four', 'five' }
testbothpairs(t)
t[6] = 'six'
testbothpairs(t)
t[4] = nil
testbothpairs(t)

-- tests of setlist table constructors
local function a(...) return ... end
print('-',unpack({a()}))
print('a',unpack({a('a')}))
print('.',unpack({a(nil)}))
print('ab',unpack({a('a', 'b')}))
print('.b',unpack({a(nil, 'a')}))
print('a.',unpack({a('a', nil)}))
print('abc',unpack({a('a', 'b', 'c')}))
print('.ab',unpack({a(nil, 'a', 'b')}))
print('a.b',unpack({a('a', nil, 'b')}))
print('ab.',unpack({a('a', 'b', nil)}))
print('..b',unpack({a(nil, nil, 'b')}))
print('a..',unpack({a('a', nil, nil)}))
print('.b.',unpack({a(nil, 'b', nil)}))
print('...',unpack({a(nil, nil, nil)}))

-- misc tests
print( # { 'abc', 'def', 'ghi', nil } ) -- should be 3 ! 
print( # { 'abc', 'def', 'ghi', false } ) -- should be 4 ! 
print( # { 'abc', 'def', 'ghi', 0 } ) -- should be 4 ! 
print( nil and 'T' or 'F' ) -- should be 'F'
print( false and 'T' or 'F' ) -- should be 'F'
print( 0 and 'T' or 'F' ) -- should be 'T'


-- basic table operation tests
local dummyfunc = function(t,...) 
	print( 'metatable call args', type(t), ...)
	return 'dummy' 
end
local makeloud = function(t)
	return setmetatable(t,{
		__index=function(t,k)
			print( '__index', type(t), k )
			return rawset(t,k)
		end,
		__newindex=function(t,k,v)
			print( '__newindex', type(t), k, v )
			rawset(t,k,v)
		end})
end
local tests = {
	{'basic table', {}},
	{'function metatable on __index', setmetatable({},{__index=dummyfunc})},
 	{'function metatable on __newindex', setmetatable({},{__newindex=dummyfunc})},
	{'plain metatable on __index', setmetatable({},makeloud({}))},
	{'plain metatable on __newindex', setmetatable({},makeloud({}))},
}
for i,test in ipairs(tests) do
	local testname = test[1]
	local testtable = test[2]
	print( '------ basic table tests on '..testname..' '..type(testtable) )
	print( 't[1]=2',     pcall( function() testtable[1]=2 end ) )
	print( 't[1]',       pcall( function() return testtable[1] end ) )
	print( 't[1]=nil',   pcall( function() testtable[1]=nil end ) )
	print( 't[1]',       pcall( function() return testtable[1] end ) )
	print( 't["a"]="b"', pcall( function() testtable["a"]="b" end ) )
	print( 't["a"],t.a', pcall( function() return testtable["a"],testtable.a end ) )
	print( 't.a="c"',    pcall( function() testtable.a="c" end ) )
	print( 't["a"],t.a', pcall( function() return testtable["a"],testtable.a end ) )
	print( 't.a=nil',    pcall( function() testtable.a=nil end ) )
	print( 't["a"],t.a', pcall( function() return testtable["a"],testtable.a end ) )
	print( 't[nil]="d"', pcall( function() testtable[nil]="d" end ) )
	print( 't[nil]',     pcall( function() return testtable[nil] end ) )
	print( 't[nil]=nil', pcall( function() testtable[nil]=nil end ) )
	print( 't[nil]',     pcall( function() return testtable[nil] end ) )
end

-- tables with doubles
local t = { [1]='a', [2]='b', [3.0]='c', [7]='d', [9]='e', [20]='f', [30.0]='g', [12.5]='h' }
print(#t)
print(t[1], t[2], t[3], t[7], t[9], t[20], t[30])
print(t[1.0], t[2.0], t[3.0], t[7.0], t[9.0], t[20.0], t[30.0], t[12.5])
local i,j,k,l,m,n,o = math.ceil(0.7),math.floor(2.1),math.abs(-3.0),math.sqrt(49.0),math.ceil(8.6),math.floor(20.5),math.max(1.2,30.0)
print(i, j, k, l, m, n, o)
print(t[i], t[j], t[k], t[l], t[m], t[n], t[o])
local half,two = .5,2
print(t[1.5-half], t[1.5+half], t[6/2], t[3.5*2], t[8.5+half], t[20.5-half], t[60*half], t[13-half])
print(#t)
