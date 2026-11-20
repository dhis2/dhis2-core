-- program_userroles linked programs to user roles. Its owning side UserRole.programs was removed
-- in 2018 and nothing reads the link; Program.userRoles is removed in the same change.
drop table if exists program_userroles;
