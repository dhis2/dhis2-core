// Fetch the login configuration dynamically
async function fetchLoginConfig() {
  try {
    const response = await fetch("./api/loginConfig");

    if (!response.ok) {
      let errorMessage = "Login failed: " + response.status;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
      } catch (jsonError) {
        console.warn("Failed to parse error response as JSON:", jsonError);
      }
      throw new Error(errorMessage);
    }

    const config = await response.json();
    document.getElementById("appTitle").innerText =
      config.applicationTitle || "DHIS2 Login";
    document.getElementById("appWelcomeMessage").innerText =
      config.applicationDescription || "Welcome to the DHIS2 application";
    if (config.countryFlag) {
      const flag = document.getElementById("flag");
      flag.src = `./dhis-web-commons/flags/${config.countryFlag}.png`;
      flag.classList.remove("hidden");
    }
  } catch (error) {
    console.error("Error:", error);
  }
}

document.addEventListener("DOMContentLoaded", () => {
  const twoFAToggle = document.getElementById("twoFAToggle");
  const twoFAContainer = document.getElementById("twoFAContainer");

  const toggleTwoFAInput = () => {
    if (twoFAToggle.checked) {
      twoFAContainer.classList.remove("hidden");
    } else {
      twoFAContainer.classList.add("hidden");
      document.getElementById("twoFA").value = "";
    }
  };

  toggleTwoFAInput();
  twoFAToggle.addEventListener("change", toggleTwoFAInput);

  document
    .getElementById("loginForm")
    .addEventListener("submit", async function (event) {
      event.preventDefault();

      const username = document.getElementById("username").value;
      const password = document.getElementById("password").value;
      const submitButton = document.querySelector('input[type="submit"]');
      const spinner = document.getElementById("spinner");
      const errorMessage = document.getElementById("errorMessage");
      const twoFA = document.getElementById("twoFA").value.trim();

      spinner.classList.remove("hidden");
      errorMessage.innerText = "";
      submitButton.disabled = true;

      let requestBody;
      if (twoFAToggle.checked && twoFA) {
        requestBody = { username, password, twoFactorCode: twoFA };
      } else {
        requestBody = { username, password };
      }

      try {
        const response = await fetch("./api/auth/login", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(requestBody),
        });

        if (!response.ok) {
          let errorMessage = "Login failed: " + response.status;
          try {
            const errorData = await response.json();
            errorMessage = errorData.message || errorMessage;
          } catch (jsonError) {
            console.warn("Failed to parse error response as JSON:", jsonError);
          }
          throw new Error(errorMessage);
        }

        const user = await response.json();

        if (user.loginStatus === "SUCCESS") {
          const redirectUrl = user.redirectUrl || "./";
          window.location.href = redirectUrl;
        } else {
          throw new Error("Login failed. Status: " + user.loginStatus);
        }
      } catch (error) {
        errorMessage.innerText = error.message;
      } finally {
        spinner.classList.add("hidden");
        submitButton.disabled = false;
      }
    });

  fetchLoginConfig();
});
