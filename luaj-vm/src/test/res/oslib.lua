-- simple os-library tests
-- these can't really be compared to C meaningfully, 
-- because they are so highly os-dependent.
print( 'os~=nil', os ~= nil )
print( '0 <= os.clock() < 100', 0 <= os.clock() and os.clock() < 100 )
print( 'type(os.date())', type(os.date()) )
print( 'os.difftime(123000, 21250)', pcall( os.difftime, 123000, 21250 ) )
--print( 'os.execute("hostname")', pcall( os.execute, 'hostname' ) )
print( 'os.execute("bogus")', pcall( os.execute, 'bogus' ) )
local s = os.getenv( 'bogus.key' )
print( 'os.getenv()', type(s)=='string' and string.match( s, "string expected" ) )
local s = os.getenv( 'bogus.key' )
print( 'os.getenv("bogus.key")', type(s) )
local s = os.getenv("PATH") or os.getenv("java.home")
print( 'os.getenv("PATH") or os.getenv("java.home")', type(s) )
local p = os.tmpname()
local q = os.tmpname()
print( 'os.tmpname()', type(p) )
print( 'os.tmpname()', type(q) )
print( 'p ~= q', p ~= q )
local s,e = pcall(os.remove, p)
print( 'os.remove(p)', not s and type(e)=='string' and string.match( e, "No such file or directory" ) )
local s,e = pcall( os.rename, p, q )
print( 'os.rename(p,q)', s and not e or e )
local s,f = pcall( io.open, p,"w" )
print( 'io.open', s, tostring(f):sub(1,5) )
print( 'write', pcall( f.write, f, "abcdef 12345" ) )
print( 'close', pcall( f.close, f ) )
print( 'os.rename(p,q)', pcall( os.rename, p, q ) )
print( 'os.remove(q)', os.remove(q) )
local s,e = os.remove(q)
print( 'os.remove(q)', s==nil and type(e)=='string' and string.match( e, "No such file or directory" ) )
print( 'os.setlocale()', pcall( os.setlocale ) )
--print( 'os.setlocale("jp")', pcall( os.setlocale, "jp" ) )
--print( 'os.setlocale("us","monetary")', pcall( os.setlocale, "us", "monetary" ) )
--print( 'os.setlocale(nil,"all")', pcall( os.setlocale, nil, "all" ) )
print( 'os.setlocale("C")', pcall( os.setlocale, "C" ) )
print( 'os.exit(123)' )
-- print( pcall( os.exit, -123 ) )
print( 'failed to exit' )