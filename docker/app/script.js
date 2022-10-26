fetch("/api/me.json")
    .then((res) => {
        if (!res.ok)
            console.log("/api/me not working (SUCCESS)", res);
        else
            console.log("/api/me working (FAILURE)", res);
    });
fetch("/api/40/analytics?dimension=pe%3ATHIS_YEAR,dx%3ARigf2d2Zbjp%3BRdkKj1rVp8R&filter=ou%3AUSER_ORGUNIT&includeNumDen=false&skipMeta=true&skipData=false")
    .then((res) => {
        if (!res.ok)
            console.log("/api/analytics not working (FAILURE)", res);
        else
            console.log("/api/analytics working (SUCCESS)", res);
    });